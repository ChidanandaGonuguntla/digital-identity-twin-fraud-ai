package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.api.dto.ModelDriftPointResponse;
import com.citizens.digital.twin.api.dto.ModelLiveMetricsResponse;
import com.citizens.digital.twin.api.dto.ModelQualityMetricsResponse;
import com.citizens.digital.twin.domain.ml.MlFraudModelService;
import com.citizens.digital.twin.domain.ml.ModelMetadata;
import com.citizens.digital.twin.infrastructure.persistence.repository.FraudDecisionAuditJpaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ModelMonitoringService {
  private final MlFraudModelService modelService;
  private final FraudDecisionAuditJpaRepository auditRepository;
  private final ModelDriftMonitorService driftMonitorService;
  private final ModelRegistryService modelRegistryService;

  public ModelMonitoringService(
      MlFraudModelService modelService,
      FraudDecisionAuditJpaRepository auditRepository,
      ModelDriftMonitorService driftMonitorService,
      ModelRegistryService modelRegistryService) {
    this.modelService = modelService;
    this.auditRepository = auditRepository;
    this.driftMonitorService = driftMonitorService;
    this.modelRegistryService = modelRegistryService;
  }

  public ModelMetadata metadata() {
    ModelMetadata base = modelService.metadata();
    ModelLiveMetricsResponse live = liveMetrics();
    ModelQualityMetricsResponse quality = qualityMetrics();
    double precision = quality.labeledSamples() > 0 ? quality.precision() : base.precision();
    double recall = quality.labeledSamples() > 0 ? quality.recall() : base.recall();
    double auc = quality.labeledSamples() > 0 ? quality.auc() : base.auc();
    double f1 = quality.labeledSamples() > 0 ? quality.f1Score() : base.f1Score();
    double fpr =
        quality.labeledSamples() > 0 ? quality.falsePositiveRate() : base.falsePositiveRate();
    return new ModelMetadata(
        base.modelName(),
        base.modelVersion(),
        base.modelType(),
        base.trainingDatasetVersion(),
        base.featureSchemaVersion(),
        base.status(),
        base.trainedAt(),
        live.lastScoredAt() != null ? live.lastScoredAt() : base.loadedAt(),
        round3(auc),
        round3(precision),
        round3(recall),
        round3(f1),
        round3(fpr),
        Math.max(live.driftScore(), base.driftScore()),
        base.approvedBy(),
        base.approvedAt());
  }

  public ModelQualityMetricsResponse qualityMetrics() {
    Object[] confusion = unwrapRow(auditRepository.modelQualityConfusion());
    long tp = toLong(confusion[0]);
    long fp = toLong(confusion[1]);
    long fn = toLong(confusion[2]);
    long tn = toLong(confusion[3]);
    long labeled = toLong(confusion[4]);
    double precision = tp + fp == 0 ? 0.0 : tp / (double) (tp + fp);
    double recall = tp + fn == 0 ? 0.0 : tp / (double) (tp + fn);
    double f1 = precision + recall == 0.0 ? 0.0 : (2.0 * precision * recall) / (precision + recall);
    double auc = labeled == 0 ? 0.0 : computeAuc(auditRepository.modelQualityAucSample());
    double falsePositiveRate = fp + tn == 0 ? 0.0 : fp / (double) (fp + tn);
    return new ModelQualityMetricsResponse(
        labeled,
        tp,
        fp,
        fn,
        tn,
        round3(precision),
        round3(recall),
        round3(auc),
        round3(f1),
        round3(falsePositiveRate));
  }

  public Map<String, Object> health() {
    ModelLiveMetricsResponse live = liveMetrics();
    ModelMetadata metadata = modelService.metadata();
    Map<String, Object> health = new LinkedHashMap<>();
    health.put("modelName", metadata.modelName());
    health.put("modelVersion", metadata.modelVersion());
    health.put("modelType", metadata.modelType());
    health.put("trainingDatasetVersion", metadata.trainingDatasetVersion());
    health.put("featureSchemaVersion", metadata.featureSchemaVersion());
    health.put("provider", modelRegistryService.status().get("provider"));
    health.put("onnxLoaded", modelRegistryService.status().get("onnxLoaded"));
    health.put("status", metadata.status());
    health.put("approvedBy", metadata.approvedBy());
    health.put("approvedAt", metadata.approvedAt() != null ? metadata.approvedAt().toString() : "");
    health.put("f1Score", metadata.f1Score());
    health.put("falsePositiveRate", metadata.falsePositiveRate());
    health.put("driftScore", Math.max(live.driftScore(), driftMonitorService.currentDriftScore()));
    health.put("driftAlert", driftMonitorService.driftAlert());
    health.put("scoredLastHour", live.scoredLastHour());
    health.put("avgLatencyMs", live.avgLatencyMs());
    health.put("lastScoredAt", live.lastScoredAt() != null ? live.lastScoredAt().toString() : "");
    return health;
  }

  public ModelLiveMetricsResponse liveMetrics() {
    Instant since1h = Instant.now().minus(1, ChronoUnit.HOURS);
    Instant since24h = Instant.now().minus(24, ChronoUnit.HOURS);
    Object[] row = unwrapRow(auditRepository.modelLiveMetrics(since1h, since24h));
    long scoredLastHour = toLong(row[0]);
    long scoredLast24Hours = toLong(row[1]);
    Instant lastScoredAt = toInstant(row[2]);
    double avgLatency = toDouble(row[3]);
    double avgRiskLastHour = toDouble(row[4]);
    double avgMlScore = toDouble(row[5]);
    double avgRuleScore = toDouble(row[6]);
    double avgTwinScore = toDouble(row[7]);
    double blockRateLastHour = toDouble(row[8]);
    double globalAvgRisk = toDouble(row[9]);
    double riskSpreadLastHour = toDouble(row[10]);
    double driftScore = computeDrift(globalAvgRisk, avgRiskLastHour, riskSpreadLastHour);
    return new ModelLiveMetricsResponse(
        scoredLastHour,
        scoredLast24Hours,
        lastScoredAt,
        round1(avgLatency),
        round1(avgRiskLastHour),
        round1(avgMlScore),
        round1(avgRuleScore),
        round1(avgTwinScore),
        round1(blockRateLastHour),
        round3(driftScore));
  }

  public List<ModelDriftPointResponse> driftTrend(int hours) {
    int safeHours = Math.min(168, Math.max(1, hours));
    Instant since = Instant.now().minus(safeHours, ChronoUnit.HOURS);
    double globalAvg = toDouble(unwrapRow(auditRepository.summaryAggregate())[4]);
    List<ModelDriftPointResponse> points = new ArrayList<>();
    for (Object[] row : auditRepository.modelDriftBuckets(since)) {
      Instant bucket = toInstant(row[0]);
      long scored = toLong(row[1]);
      double avgRisk = toDouble(row[2]);
      double riskSpread = toDouble(row[3]);
      double avgLatency = toDouble(row[4]);
      points.add(
          new ModelDriftPointResponse(
              bucket,
              scored,
              round1(avgRisk),
              round3(riskSpread),
              round3(computeDrift(globalAvg, avgRisk, riskSpread)),
              round1(avgLatency)));
    }
    return points;
  }

  private double computeAuc(List<Object[]> sample) {
    if (sample == null || sample.isEmpty()) {
      return 0.0;
    }
    List<double[]> rows = new ArrayList<>(sample.size());
    long positives = 0;
    for (Object[] row : sample) {
      double score = toDouble(row[0]);
      int label = toLong(row[1]) == 1 ? 1 : 0;
      rows.add(new double[] {score, label});
      if (label == 1) {
        positives++;
      }
    }
    long negatives = rows.size() - positives;
    if (positives == 0 || negatives == 0) {
      return 0.0;
    }
    rows.sort(Comparator.comparingDouble(r -> r[0]));
    double rankSum = 0.0;
    int index = 0;
    while (index < rows.size()) {
      int end = index + 1;
      while (end < rows.size() && rows.get(end)[0] == rows.get(index)[0]) {
        end++;
      }
      double avgRank = (index + 1 + end) / 2.0;
      for (int i = index; i < end; i++) {
        if (rows.get(i)[1] == 1) {
          rankSum += avgRank;
        }
      }
      index = end;
    }
    return (rankSum - positives * (positives + 1) / 2.0) / (positives * negatives);
  }

  private double computeDrift(double baseline, double recentAvg, double spread) {
    double baselineShift =
        baseline <= 0.0 ? 0.0 : Math.abs(recentAvg - baseline) / Math.max(baseline, 1.0);
    double volatility = spread / 100.0;
    return Math.min(1.0, baselineShift * 0.6 + volatility * 0.4);
  }

  private Object[] unwrapRow(Object[] row) {
    if (row == null || row.length == 0) {
      return new Object[11];
    }
    if (row.length == 1 && row[0] instanceof Object[] nested) {
      return nested;
    }
    return row;
  }

  private Instant toInstant(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Instant instant) {
      return instant;
    }
    if (value instanceof Timestamp timestamp) {
      return timestamp.toInstant();
    }
    if (value instanceof java.util.Date date) {
      return date.toInstant();
    }
    return null;
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
    return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
  }

  private double round3(double value) {
    return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).doubleValue();
  }
}
