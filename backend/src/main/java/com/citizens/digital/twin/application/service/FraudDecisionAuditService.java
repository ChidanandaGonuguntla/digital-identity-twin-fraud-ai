package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.api.dto.*;
import com.citizens.digital.twin.domain.ml.MlFraudPrediction;
import com.citizens.digital.twin.domain.model.Decision;
import com.citizens.digital.twin.domain.model.RiskAssessment;
import com.citizens.digital.twin.domain.model.RiskSignal;
import com.citizens.digital.twin.domain.model.ScoreBreakdown;
import com.citizens.digital.twin.domain.model.TransactionEvent;
import com.citizens.digital.twin.domain.policy.FraudPolicyConstants;
import com.citizens.digital.twin.infrastructure.persistence.entity.FraudDecisionAuditEntity;
import com.citizens.digital.twin.infrastructure.persistence.repository.FraudDecisionAuditJpaRepository;
import com.citizens.digital.twin.infrastructure.persistence.repository.FraudDecisionAuditSpecifications;
import com.citizens.digital.twin.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class FraudDecisionAuditService {
  private final FraudDecisionAuditJpaRepository repository;
  private final ObjectMapper objectMapper;

  public FraudDecisionAuditService(
      FraudDecisionAuditJpaRepository repository, ObjectMapper objectMapper) {
    this.repository = repository;
    this.objectMapper = objectMapper;
  }

  public Optional<FraudDecisionAuditEntity> findByAssessmentId(String assessmentId) {
    return repository.findById(assessmentId);
  }

  public boolean existsByTransactionId(String transactionId) {
    return repository.existsByTransactionId(transactionId);
  }

  public Optional<FraudDecisionResponse> findExistingResponse(String transactionId) {
    return repository
        .findFirstByTransactionIdOrderByAssessedAtDesc(transactionId)
        .map(this::toFraudDecisionResponse);
  }

  public void updateDecision(String assessmentId, String decision) {
    FraudDecisionAuditEntity entity =
        repository
            .findById(assessmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Audit record not found"));
    entity.setDecision(decision);
    repository.save(entity);
  }

  public TransactionEvent parseTransactionEvent(FraudDecisionAuditEntity entity) {
    TransactionEvent event = tryParseTransactionEvent(entity);
    if (event == null) {
      throw new ResourceNotFoundException("Unable to parse transaction snapshot for assessment");
    }
    return event;
  }

  public void save(
      TransactionEvent event,
      RiskAssessment assessment,
      MlFraudPrediction mlPrediction,
      boolean twinUpdated,
      boolean challenged) {
    save(event, assessment, mlPrediction, twinUpdated, challenged, null);
  }

  public void save(
      TransactionEvent event,
      RiskAssessment assessment,
      MlFraudPrediction mlPrediction,
      boolean twinUpdated,
      boolean challenged,
      com.citizens.digital.twin.domain.ml.ChampionChallengerOutcome championChallenger) {
    FraudDecisionAuditEntity e = new FraudDecisionAuditEntity();
    e.setAssessmentId(assessment.assessmentId());
    e.setTransactionId(assessment.transactionId());
    e.setCustomerId(assessment.customerId());
    e.setDecision(assessment.decision().name());
    e.setFinalScore(assessment.scoreBreakdown().finalScore());
    e.setScoreBreakdownJson(write(assessment.scoreBreakdown()));
    e.setReasonCodesJson(write(assessment.reasonCodes()));
    e.setEventSnapshotJson(writeEventSnapshot(event));
    e.setAmount(BigDecimal.valueOf(event.amount()));
    e.setMerchantCategory(blankToNull(event.merchantCategory()));
    e.setDeviceId(blankToNull(event.deviceId()));
    e.setModelVersion(assessment.modelVersion());
    e.setPolicyVersion(assessment.policyVersion());
    e.setFeatureVersion(FraudPolicyConstants.FEATURE_VERSION);
    e.setFinalDecisionReason(buildFinalDecisionReason(assessment));
    e.setFeatureVectorJson(write(mlPrediction.featureVector()));
    e.setChallenged(challenged);
    e.setTwinUpdated(twinUpdated);
    e.setLatencyMs(assessment.latencyMs());
    e.setAssessedAt(assessment.assessedAt());
    e.setCreatedAt(Instant.now());
    if (championChallenger != null) {
      e.setChampionScore(championChallenger.champion().score());
      e.setChampionModelVersion(championChallenger.champion().modelVersion());
      if (championChallenger.challenger() != null) {
        e.setChallengerScore(championChallenger.challenger().score());
        e.setChallengerModelVersion(championChallenger.challenger().modelVersion());
        e.setScoreDelta(championChallenger.scoreDelta());
        e.setModelAgreement(championChallenger.modelAgreement());
      }
    }
    repository.save(e);
  }

  public FraudDecisionAuditDetailResponse getDetail(String assessmentId) {
    FraudDecisionAuditEntity entity =
        repository
            .findById(assessmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Audit record not found"));
    return toDetail(entity);
  }

  public AuditPageResponse byCustomer(String customerId, int page, int size) {
    int safePage = Math.max(0, page);
    int safeSize = Math.min(200, Math.max(1, size));
    Page<FraudDecisionAuditEntity> result =
        repository.findByCustomerIdOrderByAssessedAtDesc(customerId, PageRequest.of(safePage, safeSize));
    return toPage(result);
  }

  public AuditPageResponse byTransaction(String transactionId, int page, int size) {
    int safePage = Math.max(0, page);
    int safeSize = Math.min(200, Math.max(1, size));
    Page<FraudDecisionAuditEntity> result =
        repository.findByTransactionIdOrderByAssessedAtDesc(
            transactionId, PageRequest.of(safePage, safeSize));
    return toPage(result);
  }

  public List<FraudDecisionAuditEntity> recent() {
    return repository.findTop50ByOrderByAssessedAtDesc();
  }

  public AuditSummaryResponse summary() {
    Object[] row = unwrapAggregateRow(repository.summaryAggregate());
    long total = toLong(row[0]);
    long blocked = toLong(row[1]);
    long reviews = toLong(row[2]);
    long allowed = toLong(row[3]);
    double avgScore = toDouble(row[4]);
    double blockRate = total == 0 ? 0.0 : (blocked * 100.0) / total;
    return new AuditSummaryResponse(
        total,
        blocked,
        reviews,
        allowed,
        round1(avgScore),
        round1(blockRate),
        Math.round(toDouble(repository.p95Latency())),
        round2(repository.preventedAmount()));
  }

  public AuditPageResponse stepUpQueue(int page, int size) {
    int safePage = Math.max(0, page);
    int safeSize = Math.min(200, Math.max(1, size));
    PageRequest pageable = PageRequest.of(safePage, safeSize);
    Page<FraudDecisionAuditEntity> result = repository.findStepUpQueue(pageable);
    List<AuditDecisionResponse> items = result.getContent().stream().map(this::toResponse).toList();
    return new AuditPageResponse(
        items, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
  }

  public AuditPageResponse page(
      int page,
      int size,
      String decision,
      String customerId,
      String transactionId,
      Instant from,
      Instant to,
      Double minScore,
      Double maxScore) {
    int safePage = Math.max(0, page);
    int safeSize = Math.min(200, Math.max(1, size));
    PageRequest pageable =
        PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "assessedAt"));
    String normalizedDecision =
        decision == null || decision.isBlank() || "ALL".equalsIgnoreCase(decision)
            ? null
            : decision.toUpperCase();
    Page<FraudDecisionAuditEntity> result =
        repository.findAll(
            FraudDecisionAuditSpecifications.withFilters(
                normalizedDecision,
                blankToNull(customerId),
                blankToNull(transactionId),
                from,
                to,
                minScore == null ? null : BigDecimal.valueOf(minScore),
                maxScore == null ? null : BigDecimal.valueOf(maxScore)),
            pageable);
    return toPage(result);
  }

  public List<AuditTrendPointResponse> trends(int hours) {
    int safeHours = Math.min(168, Math.max(1, hours));
    Instant since = Instant.now().minus(safeHours, ChronoUnit.HOURS);
    List<AuditTrendPointResponse> points = new ArrayList<>();
    for (Object[] row : repository.trendBuckets(since)) {
      Instant bucket = toInstant(row[0]);
      points.add(
          new AuditTrendPointResponse(
              bucket, toLong(row[1]), toLong(row[2]), toLong(row[3])));
    }
    return points;
  }

  public List<AuditScoreBucketResponse> scoreDistribution() {
    List<AuditScoreBucketResponse> buckets = new ArrayList<>();
    for (Object[] row : repository.scoreBuckets()) {
      int start = ((Number) row[0]).intValue();
      long count = toLong(row[1]);
      buckets.add(new AuditScoreBucketResponse(start + "-" + (start + 9), count));
    }
    return buckets;
  }

  public List<AuditReasonLeaderboardItem> reasonLeaderboard() {
    List<AuditReasonLeaderboardItem> items = new ArrayList<>();
    for (Object[] row : repository.reasonLeaderboard()) {
      items.add(
          new AuditReasonLeaderboardItem(
              String.valueOf(row[0]), round1(toDouble(row[1])), toLong(row[2])));
    }
    return items;
  }

  public String exportDecisionsCsv(
      String decision,
      String customerId,
      String transactionId,
      Instant from,
      Instant to,
      Double minScore,
      Double maxScore) {
    Page<FraudDecisionAuditEntity> result =
        repository.findAll(
            FraudDecisionAuditSpecifications.withFilters(
                normalizeDecisionFilter(decision),
                blankToNull(customerId),
                blankToNull(transactionId),
                from,
                to,
                minScore == null ? null : BigDecimal.valueOf(minScore),
                maxScore == null ? null : BigDecimal.valueOf(maxScore)),
            PageRequest.of(0, 10_000, Sort.by(Sort.Direction.DESC, "assessedAt")));
    StringBuilder csv =
        new StringBuilder(
            "assessmentId,transactionId,customerId,decision,riskScore,amount,merchantCategory,deviceId,latitude,longitude,latencyMs,assessedAt,modelVersion,policyVersion,featureVersion,finalDecisionReason,coldStart,challenged,twinUpdated,reasons\n");
    for (FraudDecisionAuditEntity entity : result.getContent()) {
      AuditDecisionResponse item = toResponse(entity);
      csv.append(csvEscape(item.assessmentId())).append(',');
      csv.append(csvEscape(item.transactionId())).append(',');
      csv.append(csvEscape(item.customerId())).append(',');
      csv.append(csvEscape(item.decision())).append(',');
      csv.append(item.riskScore()).append(',');
      csv.append(item.amount()).append(',');
      csv.append(csvEscape(item.merchantCategory())).append(',');
      csv.append(csvEscape(item.deviceId())).append(',');
      csv.append(item.latitude()).append(',');
      csv.append(item.longitude()).append(',');
      csv.append(item.latencyMs()).append(',');
      csv.append(item.assessedAt()).append(',');
      csv.append(csvEscape(item.modelVersion())).append(',');
      csv.append(csvEscape(item.policyVersion())).append(',');
      csv.append(csvEscape(item.featureVersion())).append(',');
      csv.append(csvEscape(item.finalDecisionReason())).append(',');
      csv.append(item.coldStart()).append(',');
      csv.append(item.challenged()).append(',');
      csv.append(item.twinUpdated()).append(',');
      csv.append(csvEscape(String.join(" | ", item.reasons()))).append('\n');
    }
    return csv.toString();
  }

  private String normalizeDecisionFilter(String decision) {
    return decision == null || decision.isBlank() || "ALL".equalsIgnoreCase(decision)
        ? null
        : decision.toUpperCase();
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

  private FraudDecisionResponse toFraudDecisionResponse(FraudDecisionAuditEntity entity) {
    ScoreBreakdown breakdown = readScoreBreakdown(entity.getScoreBreakdownJson());
    List<RiskSignal> signals = readReasons(entity.getReasonCodesJson());
    TransactionEvent event = parseTransactionEvent(entity);
    List<String> reasons =
        signals.stream().map(RiskSignal::message).filter(m -> m != null && !m.isBlank()).toList();
    boolean coldStart =
        signals.stream().anyMatch(s -> "TWIN_COLD_START".equalsIgnoreCase(s.code()));
    long eventTime =
        event.timestamp() != null
            ? event.timestamp().toEpochMilli()
            : entity.getAssessedAt().toEpochMilli();
    DecisionEvent decisionEvent =
        new DecisionEvent(
            entity.getAssessmentId(),
            entity.getTransactionId(),
            entity.getCustomerId(),
            event.amount(),
            event.merchantCategory(),
            event.deviceId(),
            event.latitude(),
            event.longitude(),
            eventTime,
            entity.getFinalScore() == null ? 0.0 : entity.getFinalScore().doubleValue(),
            entity.getDecision(),
            coldStart,
            List.of(),
            reasons);
    RiskAssessment assessment =
        new RiskAssessment(
            entity.getAssessmentId(),
            entity.getTransactionId(),
            entity.getCustomerId(),
            Decision.valueOf(entity.getDecision()),
            breakdown,
            signals,
            entity.getModelVersion(),
            entity.getPolicyVersion(),
            entity.getLatencyMs(),
            entity.getAssessedAt());
    return FraudDecisionResponse.from(assessment, decisionEvent);
  }

  private AuditPageResponse toPage(Page<FraudDecisionAuditEntity> result) {
    List<AuditDecisionResponse> items = result.getContent().stream().map(this::toResponse).toList();
    return new AuditPageResponse(
        items, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
  }

  private FraudDecisionAuditDetailResponse toDetail(FraudDecisionAuditEntity entity) {
    List<RiskSignal> signals = readReasons(entity.getReasonCodesJson());
    return new FraudDecisionAuditDetailResponse(
        entity.getAssessmentId(),
        entity.getTransactionId(),
        entity.getCustomerId(),
        entity.getDecision(),
        entity.getFinalScore() == null ? 0.0 : entity.getFinalScore().doubleValue(),
        readScoreBreakdown(entity.getScoreBreakdownJson()),
        signals,
        parseTransactionEvent(entity),
        entity.getModelVersion(),
        entity.getPolicyVersion(),
        entity.getFeatureVersion(),
        entity.getFinalDecisionReason(),
        readFeatureVector(entity.getFeatureVectorJson()),
        entity.isChallenged(),
        entity.isTwinUpdated(),
        entity.getLatencyMs(),
        entity.getAssessedAt(),
        entity.getChampionScore() == null ? null : entity.getChampionScore().doubleValue(),
        entity.getChallengerScore() == null ? null : entity.getChallengerScore().doubleValue(),
        entity.getScoreDelta() == null ? null : entity.getScoreDelta().doubleValue(),
        entity.getModelAgreement(),
        entity.getChampionModelVersion(),
        entity.getChallengerModelVersion());
  }

  private AuditDecisionResponse toResponse(FraudDecisionAuditEntity entity) {
    SnapshotView snapshotView = resolveSnapshot(entity);
    List<RiskSignal> signals = readReasons(entity.getReasonCodesJson());
    List<String> reasons =
        signals.stream().map(RiskSignal::message).filter(m -> m != null && !m.isBlank()).toList();
    boolean coldStart =
        signals.stream().anyMatch(s -> "TWIN_COLD_START".equalsIgnoreCase(s.code()));
    return new AuditDecisionResponse(
        entity.getAssessmentId(),
        entity.getTransactionId(),
        entity.getCustomerId(),
        entity.getDecision(),
        entity.getFinalScore() == null ? 0.0 : entity.getFinalScore().doubleValue(),
        snapshotView.amount(),
        snapshotView.merchantCategory(),
        snapshotView.deviceId(),
        snapshotView.latitude(),
        snapshotView.longitude(),
        entity.getLatencyMs(),
        entity.getAssessedAt(),
        reasons,
        coldStart,
        entity.getModelVersion(),
        entity.getPolicyVersion(),
        entity.getFeatureVersion(),
        entity.getFinalDecisionReason(),
        entity.isChallenged(),
        entity.isTwinUpdated());
  }

  private record SnapshotView(
      double amount, String merchantCategory, String deviceId, double latitude, double longitude) {}

  private SnapshotView resolveSnapshot(FraudDecisionAuditEntity entity) {
    JsonNode snapshot = readJson(entity.getEventSnapshotJson());
    TransactionEvent event = tryParseTransactionEvent(entity);
    double amount =
        entity.getAmount() != null && entity.getAmount().doubleValue() > 0
            ? entity.getAmount().doubleValue()
            : event != null && event.amount() > 0
                ? event.amount()
                : numberFrom(snapshot, "amount", "transactionAmount", "txnAmount");
    String merchantCategory =
        firstNonBlank(
            entity.getMerchantCategory(),
            event != null ? event.merchantCategory() : null,
            text(snapshot, "merchantCategory"),
            text(snapshot, "merchant_category"),
            text(snapshot, "category"));
    String deviceId =
        firstNonBlank(
            entity.getDeviceId(),
            event != null ? event.deviceId() : null,
            text(snapshot, "deviceId"),
            text(snapshot, "device_id"));
    double latitude =
        event != null && event.latitude() != 0.0
            ? event.latitude()
            : numberFrom(snapshot, "latitude");
    double longitude =
        event != null && event.longitude() != 0.0
            ? event.longitude()
            : numberFrom(snapshot, "longitude");
    return new SnapshotView(amount, merchantCategory, deviceId, latitude, longitude);
  }

  private TransactionEvent tryParseTransactionEvent(FraudDecisionAuditEntity entity) {
    try {
      String json = entity.getEventSnapshotJson();
      if (json == null || json.isBlank() || "{}".equals(json.trim())) {
        return null;
      }
      return objectMapper.readValue(json, TransactionEvent.class);
    } catch (Exception ex) {
      return null;
    }
  }

  private String buildFinalDecisionReason(RiskAssessment assessment) {
    Decision decision = assessment.decision();
    List<RiskSignal> ranked =
        assessment.reasonCodes().stream()
            .sorted(Comparator.comparing(RiskSignal::scoreContribution).reversed())
            .limit(3)
            .toList();
    if (ranked.isEmpty()) {
      return decision.name();
    }
    String signals =
        ranked.stream()
            .map(RiskSignal::message)
            .filter(m -> m != null && !m.isBlank())
            .collect(Collectors.joining("; "));
    return decision.name() + ": " + signals;
  }

  private ScoreBreakdown readScoreBreakdown(String json) {
    try {
      return json == null || json.isBlank()
          ? new ScoreBreakdown(
              BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
          : objectMapper.readValue(json, ScoreBreakdown.class);
    } catch (Exception ex) {
      return new ScoreBreakdown(
          BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
  }

  private Map<String, Double> readFeatureVector(String json) {
    try {
      return json == null || json.isBlank()
          ? Map.of()
          : objectMapper.readValue(json, new TypeReference<Map<String, Double>>() {});
    } catch (Exception ex) {
      return Map.of();
    }
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
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

  private List<RiskSignal> readReasons(String json) {
    try {
      return json == null || json.isBlank()
          ? List.of()
          : objectMapper.readValue(json, new TypeReference<List<RiskSignal>>() {});
    } catch (Exception ex) {
      return List.of();
    }
  }

  private String text(JsonNode node, String field) {
    JsonNode value = node.get(field);
    return value == null || value.isNull() ? "" : value.asText("");
  }

  private double numberFrom(JsonNode node, String... fields) {
    for (String field : fields) {
      JsonNode value = node.path(field);
      if (value.isMissingNode() || value.isNull()) {
        continue;
      }
      if (value.isNumber()) {
        return value.asDouble();
      }
      if (value.isTextual()) {
        try {
          return Double.parseDouble(value.asText().trim());
        } catch (Exception ignored) {
        }
      }
    }
    return 0.0;
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return "";
  }

  private Object[] unwrapAggregateRow(Object[] row) {
    if (row == null || row.length == 0) {
      return new Object[] {0L, 0L, 0L, 0L, 0.0};
    }
    Object current = row;
    while (current instanceof Object[] nested && nested.length == 1 && nested[0] instanceof Object[]) {
      current = nested[0];
    }
    if (current instanceof Object[] flat) {
      return flat.length >= 5 ? flat : row;
    }
    return row;
  }

  private Instant toInstant(Object value) {
    if (value instanceof Instant instant) {
      return instant;
    }
    if (value instanceof Timestamp timestamp) {
      return timestamp.toInstant();
    }
    if (value instanceof java.util.Date date) {
      return date.toInstant();
    }
    return Instant.now();
  }

  private long toLong(Object value) {
    if (value == null) {
      return 0L;
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value instanceof Object[] nested && nested.length > 0) {
      return toLong(nested[0]);
    }
    return 0L;
  }

  private double toDouble(Object value) {
    if (value == null) {
      return 0.0;
    }
    if (value instanceof Number number) {
      return number.doubleValue();
    }
    if (value instanceof Object[] nested && nested.length > 0) {
      return toDouble(nested[0]);
    }
    return 0.0;
  }

  private double round1(double value) {
    return BigDecimal.valueOf(value).setScale(1, java.math.RoundingMode.HALF_UP).doubleValue();
  }

  private double round2(double value) {
    return BigDecimal.valueOf(value).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
  }

  private String write(Object v) {
    try {
      return objectMapper.writeValueAsString(v);
    } catch (Exception ex) {
      return "{}";
    }
  }

  private String writeEventSnapshot(TransactionEvent event) {
    String json = write(event);
    if (!isObjectJson(json)) {
      throw new IllegalStateException(
          "Unable to persist transaction snapshot for transactionId=" + event.transactionId());
    }
    return json;
  }

  private boolean isObjectJson(String json) {
    if (json == null || json.isBlank()) {
      return false;
    }
    try {
      JsonNode node = objectMapper.readTree(json);
      return node != null && node.isObject();
    } catch (Exception ex) {
      return false;
    }
  }
}
