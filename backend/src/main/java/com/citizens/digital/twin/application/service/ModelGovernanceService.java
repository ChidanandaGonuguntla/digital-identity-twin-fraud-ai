package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.api.dto.BiasFairnessReviewResponse;
import com.citizens.digital.twin.api.dto.BiasSegmentReviewResponse;
import com.citizens.digital.twin.api.dto.DataDriftSummaryResponse;
import com.citizens.digital.twin.api.dto.FeatureContributionResponse;
import com.citizens.digital.twin.api.dto.FeatureDriftMetricResponse;
import com.citizens.digital.twin.api.dto.FraudDecisionAuditDetailResponse;
import com.citizens.digital.twin.api.dto.ModelExplainabilityReportResponse;
import com.citizens.digital.twin.api.dto.ModelQualityMetricsResponse;
import com.citizens.digital.twin.api.dto.ModelRegistryEntryResponse;
import com.citizens.digital.twin.domain.ml.ModelMetadata;
import com.citizens.digital.twin.domain.ml.ModelRegistryStatus;
import com.citizens.digital.twin.domain.policy.FraudPolicyConstants;
import com.citizens.digital.twin.infrastructure.persistence.entity.ModelRegistryEntity;
import com.citizens.digital.twin.infrastructure.persistence.repository.FraudDecisionAuditJpaRepository;
import com.citizens.digital.twin.infrastructure.persistence.repository.ModelRegistryJpaRepository;
import com.citizens.digital.twin.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModelGovernanceService {
  private final ModelRegistryJpaRepository registryRepository;
  private final ModelMonitoringService monitoringService;
  private final ModelDriftMonitorService driftMonitorService;
  private final FraudDecisionAuditService auditService;
  private final FraudDecisionAuditJpaRepository auditRepository;
  private final AnalystActionAuditService analystActionAuditService;
  private final ModelRegistryService registryService;
  private final ObjectMapper objectMapper;

  public ModelGovernanceService(
      ModelRegistryJpaRepository registryRepository,
      ModelMonitoringService monitoringService,
      ModelDriftMonitorService driftMonitorService,
      FraudDecisionAuditService auditService,
      FraudDecisionAuditJpaRepository auditRepository,
      AnalystActionAuditService analystActionAuditService,
      ModelRegistryService registryService,
      ObjectMapper objectMapper) {
    this.registryRepository = registryRepository;
    this.monitoringService = monitoringService;
    this.driftMonitorService = driftMonitorService;
    this.auditService = auditService;
    this.auditRepository = auditRepository;
    this.analystActionAuditService = analystActionAuditService;
    this.registryService = registryService;
    this.objectMapper = objectMapper;
  }

  public List<ModelRegistryEntryResponse> registry() {
    return registryRepository.findAllByOrderByDeployedAtDesc().stream()
        .map(this::toRegistryEntry)
        .toList();
  }

  public ModelMetadata governanceMetadata() {
    ModelMetadata live = monitoringService.metadata();
    Optional<ModelRegistryEntity> active = registryRepository.findFirstByActiveTrue();
    if (active.isEmpty()) {
      return live;
    }
    ModelRegistryEntity entity = active.get();
    Map<String, Double> metrics = readMetrics(entity.getMetricsJson());
    ModelQualityMetricsResponse quality = monitoringService.qualityMetrics();
    double fpr =
        quality.falsePositiveRate() > 0 ? quality.falsePositiveRate() : computeFpr(metrics);
    return new ModelMetadata(
        entity.getModelName(),
        entity.getModelVersion(),
        live.modelType(),
        defaultString(entity.getTrainingDatasetVersion(), "fraud-synthetic-v1.0.0"),
        defaultString(entity.getFeatureSchemaVersion(), FraudPolicyConstants.FEATURE_VERSION),
        entity.getRegistryStatus(),
        entity.getTrainedAt() != null ? entity.getTrainedAt() : live.trainedAt(),
        live.loadedAt(),
        metric(metrics, "auc", quality.auc()),
        metric(metrics, "precision", quality.precision()),
        metric(metrics, "recall", quality.recall()),
        metric(metrics, "f1Score", quality.f1Score()),
        fpr,
        Math.max(live.driftScore(), driftMonitorService.currentDriftScore()),
        entity.getApprovedBy(),
        entity.getApprovedAt());
  }

  public DataDriftSummaryResponse dataDrift(int hours) {
    ModelRegistryEntity active =
        registryRepository
            .findFirstByActiveTrue()
            .orElseThrow(() -> new ResourceNotFoundException("No active model in registry"));
    Instant since = Instant.now().minus(Math.max(1, hours), ChronoUnit.HOURS);
    List<String> vectors = auditRepository.recentFeatureVectors(since, 5000);
    List<String> baselineVectors = auditRepository.baselineFeatureVectors(2000);
    List<String> featureOrder = readFeatureOrder(active.getFeatureOrderJson());
    List<FeatureDriftMetricResponse> features = new ArrayList<>();
    double peakDrift = 0.0;
    for (String feature : featureOrder) {
      double baselineMean = meanFeature(baselineVectors, feature);
      double currentMean = meanFeature(vectors, feature);
      double driftIndex = driftIndex(baselineMean, currentMean);
      peakDrift = Math.max(peakDrift, driftIndex);
      features.add(
          new FeatureDriftMetricResponse(
              feature,
              round3(baselineMean),
              round3(currentMean),
              round3(driftIndex),
              driftSeverity(driftIndex)));
    }
    double modelDrift = Math.max(driftMonitorService.currentDriftScore(), peakDrift);
    return new DataDriftSummaryResponse(
        active.getModelVersion(),
        defaultString(active.getFeatureSchemaVersion(), FraudPolicyConstants.FEATURE_VERSION),
        round3(modelDrift),
        modelDrift >= 0.35,
        Instant.now(),
        features);
  }

  public BiasFairnessReviewResponse biasReview() {
    ModelRegistryEntity active =
        registryRepository
            .findFirstByActiveTrue()
            .orElseThrow(() -> new ResourceNotFoundException("No active model in registry"));
    JsonNode review = readJson(active.getBiasReviewJson());
    List<BiasSegmentReviewResponse> segments = new ArrayList<>();
    for (Object[] row : auditRepository.biasSegmentMetrics()) {
      long samples = toLong(row[1]);
      if (samples == 0) {
        continue;
      }
      double blockRate = toDouble(row[2]);
      double fpr = toDouble(row[3]);
      segments.add(
          new BiasSegmentReviewResponse(
              String.valueOf(row[0]),
              samples,
              round1(blockRate),
              round3(fpr),
              fpr > 0.15 ? "REVIEW_REQUIRED" : "WITHIN_TOLERANCE"));
    }
    return new BiasFairnessReviewResponse(
        text(review, "reviewStatus", "PENDING_SCHEDULED"),
        parseInstant(review.path("lastReviewedAt").asText(null)),
        active.getModelVersion(),
        segments,
        text(review, "notes", "Bias and fairness review placeholder for model risk committee."));
  }

  public ModelExplainabilityReportResponse explainability(String assessmentId) {
    FraudDecisionAuditDetailResponse detail = auditService.getDetail(assessmentId);
    List<FeatureContributionResponse> topFeatures =
        detail.featureVector().entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
            .limit(8)
            .map(
                entry ->
                    new FeatureContributionResponse(
                        entry.getKey(),
                        round3(entry.getValue()),
                        round3(entry.getValue() * detail.finalScore() / 100.0)))
            .toList();
    return new ModelExplainabilityReportResponse(
        detail.assessmentId(),
        detail.transactionId(),
        detail.customerId(),
        detail.decision(),
        detail.finalScore(),
        detail.modelVersion(),
        defaultString(detail.featureVersion(), FraudPolicyConstants.FEATURE_VERSION),
        detail.policyVersion(),
        detail.finalDecisionReason(),
        detail.scoreBreakdown(),
        detail.reasonCodes(),
        detail.featureVector(),
        topFeatures,
        detail.assessedAt());
  }

  @Transactional
  public ModelRegistryEntryResponse approve(String modelVersion, String note) {
    ModelRegistryEntity entity = requireVersion(modelVersion);
    if (ModelRegistryStatus.REJECTED.name().equals(entity.getRegistryStatus())) {
      throw new IllegalStateException("Rejected model cannot be approved without resubmission");
    }
    entity.setRegistryStatus(ModelRegistryStatus.APPROVED.name());
    entity.setApprovedBy(currentActor());
    entity.setApprovedAt(Instant.now());
    entity.setRejectionReason(null);
    registryRepository.save(entity);
    analystActionAuditService.record(
        "MODEL_APPROVE",
        "MODEL",
        modelVersion,
        Map.of("note", note == null ? "" : note, "approvedBy", entity.getApprovedBy()));
    return toRegistryEntry(entity);
  }

  @Transactional
  public ModelRegistryEntryResponse reject(String modelVersion, String reason) {
    ModelRegistryEntity entity = requireVersion(modelVersion);
    if (entity.isActive()) {
      throw new IllegalStateException("Active model cannot be rejected");
    }
    entity.setRegistryStatus(ModelRegistryStatus.REJECTED.name());
    entity.setRejectionReason(reason);
    entity.setApprovedBy(null);
    entity.setApprovedAt(null);
    registryRepository.save(entity);
    analystActionAuditService.record(
        "MODEL_REJECT", "MODEL", modelVersion, Map.of("reason", reason == null ? "" : reason));
    return toRegistryEntry(entity);
  }

  @Transactional
  public ModelRegistryEntryResponse submitForApproval(String modelVersion) {
    ModelRegistryEntity entity = requireVersion(modelVersion);
    entity.setRegistryStatus(ModelRegistryStatus.PENDING_APPROVAL.name());
    entity.setApprovedBy(null);
    entity.setApprovedAt(null);
    entity.setRejectionReason(null);
    registryRepository.save(entity);
    analystActionAuditService.record("MODEL_SUBMIT_APPROVAL", "MODEL", modelVersion, Map.of());
    return toRegistryEntry(entity);
  }

  @Transactional
  public ModelRegistryEntryResponse retire(String modelVersion) {
    registryService.retireVersion(modelVersion);
    analystActionAuditService.record("MODEL_RETIRE", "MODEL", modelVersion, Map.of());
    return toRegistryEntry(requireVersion(modelVersion));
  }

  private ModelRegistryEntity requireVersion(String modelVersion) {
    ModelRegistryEntity.ModelRegistryId id = new ModelRegistryEntity.ModelRegistryId();
    id.setModelName("digital-twin-fraud-risk");
    id.setModelVersion(modelVersion);
    return registryRepository
        .findById(id)
        .orElseThrow(
            () -> new ResourceNotFoundException("Model version not found: " + modelVersion));
  }

  private ModelRegistryEntryResponse toRegistryEntry(ModelRegistryEntity entity) {
    Map<String, Double> metrics = readMetrics(entity.getMetricsJson());
    return new ModelRegistryEntryResponse(
        entity.getModelName(),
        entity.getModelVersion(),
        defaultString(entity.getTrainingDatasetVersion(), "fraud-synthetic-v1.0.0"),
        defaultString(entity.getFeatureSchemaVersion(), FraudPolicyConstants.FEATURE_VERSION),
        entity.getRegistryStatus(),
        entity.isActive(),
        metric(metrics, "auc", 0.0),
        metric(metrics, "precision", 0.0),
        metric(metrics, "recall", 0.0),
        metric(metrics, "f1Score", 0.0),
        metric(metrics, "falsePositiveRate", computeFpr(metrics)),
        driftMonitorService.currentDriftScore(),
        entity.getApprovedBy(),
        entity.getApprovedAt(),
        entity.getTrainedAt(),
        entity.getDeployedAt(),
        entity.getRejectionReason());
  }

  private Map<String, Double> readMetrics(String json) {
    try {
      return json == null || json.isBlank()
          ? Map.of()
          : objectMapper.readValue(json, new TypeReference<Map<String, Double>>() {});
    } catch (Exception ex) {
      return Map.of();
    }
  }

  private List<String> readFeatureOrder(String json) {
    try {
      return json == null || json.isBlank()
          ? List.of()
          : objectMapper.readValue(json, new TypeReference<List<String>>() {});
    } catch (Exception ex) {
      return List.of();
    }
  }

  private double meanFeature(List<String> vectors, String feature) {
    if (vectors.isEmpty()) {
      return 0.0;
    }
    double sum = 0.0;
    int count = 0;
    for (String vectorJson : vectors) {
      Map<String, Double> vector = readMetrics(vectorJson);
      if (vector.containsKey(feature)) {
        sum += vector.get(feature);
        count++;
      }
    }
    return count == 0 ? 0.0 : sum / count;
  }

  private double driftIndex(double baseline, double current) {
    double denominator = Math.max(Math.abs(baseline), 0.0001);
    return Math.abs(current - baseline) / denominator;
  }

  private String driftSeverity(double driftIndex) {
    if (driftIndex >= 0.35) {
      return "HIGH";
    }
    if (driftIndex >= 0.15) {
      return "MEDIUM";
    }
    return "LOW";
  }

  private double computeFpr(Map<String, Double> metrics) {
    return metrics.getOrDefault("falsePositiveRate", 0.0);
  }

  private double metric(Map<String, Double> metrics, String key, double fallback) {
    return metrics.containsKey(key) ? metrics.get(key) : fallback;
  }

  private String defaultString(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private JsonNode readJson(String json) {
    try {
      return json == null || json.isBlank()
          ? objectMapper.createObjectNode()
          : objectMapper.readTree(json);
    } catch (Exception ex) {
      return objectMapper.createObjectNode();
    }
  }

  private String text(JsonNode node, String field, String fallback) {
    JsonNode value = node.get(field);
    return value == null || value.isNull() || value.asText().isBlank() ? fallback : value.asText();
  }

  private Instant parseInstant(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Instant.parse(value);
    } catch (Exception ex) {
      return null;
    }
  }

  public String exportAuditReport() {
    StringBuilder csv =
        new StringBuilder(
            "modelName,modelVersion,trainingDatasetVersion,featureSchemaVersion,status,active,auc,precision,recall,f1Score,falsePositiveRate,driftScore,approvedBy,approvedAt,trainedAt,deployedAt,rejectionReason\n");
    for (ModelRegistryEntryResponse entry : registry()) {
      csv.append(csvEscape(entry.modelName())).append(',');
      csv.append(csvEscape(entry.modelVersion())).append(',');
      csv.append(csvEscape(entry.trainingDatasetVersion())).append(',');
      csv.append(csvEscape(entry.featureSchemaVersion())).append(',');
      csv.append(csvEscape(entry.status())).append(',');
      csv.append(entry.active()).append(',');
      csv.append(entry.auc()).append(',');
      csv.append(entry.precision()).append(',');
      csv.append(entry.recall()).append(',');
      csv.append(entry.f1Score()).append(',');
      csv.append(entry.falsePositiveRate()).append(',');
      csv.append(entry.driftScore()).append(',');
      csv.append(csvEscape(entry.approvedBy())).append(',');
      csv.append(entry.approvedAt() == null ? "" : entry.approvedAt()).append(',');
      csv.append(entry.trainedAt() == null ? "" : entry.trainedAt()).append(',');
      csv.append(entry.deployedAt() == null ? "" : entry.deployedAt()).append(',');
      csv.append(csvEscape(entry.rejectionReason())).append('\n');
    }
    return csv.toString();
  }

  private String currentActor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication == null || authentication.getPrincipal() == null
        ? "system"
        : String.valueOf(authentication.getPrincipal());
  }

  private String csvEscape(String value) {
    if (value == null) {
      return "";
    }
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }

  private long toLong(Object value) {
    return value == null ? 0L : ((Number) value).longValue();
  }

  private double toDouble(Object value) {
    return value == null ? 0.0 : ((Number) value).doubleValue();
  }

  private double round1(double value) {
    return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
  }

  private double round3(double value) {
    return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).doubleValue();
  }
}
