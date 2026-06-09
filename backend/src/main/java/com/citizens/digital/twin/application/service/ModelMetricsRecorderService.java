package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.domain.ml.MlFraudPrediction;
import com.citizens.digital.twin.domain.model.RiskAssessment;
import com.citizens.digital.twin.domain.model.TransactionEvent;
import com.citizens.digital.twin.infrastructure.persistence.entity.ModelMetricsEntity;
import com.citizens.digital.twin.infrastructure.persistence.repository.ModelMetricsJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ModelMetricsRecorderService {
  private final ModelMetricsJpaRepository repository;
  private final ModelDriftMonitorService driftMonitorService;
  private final ObjectMapper objectMapper;

  public ModelMetricsRecorderService(
      ModelMetricsJpaRepository repository,
      ModelDriftMonitorService driftMonitorService,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.driftMonitorService = driftMonitorService;
    this.objectMapper = objectMapper;
  }

  public void record(
      TransactionEvent event, RiskAssessment assessment, MlFraudPrediction mlPrediction) {
    driftMonitorService.record(mlPrediction);
    ModelMetricsEntity entity = new ModelMetricsEntity();
    entity.setMetricId(UUID.randomUUID().toString());
    entity.setAssessmentId(assessment.assessmentId());
    entity.setModelName("digital-twin-fraud-risk");
    entity.setModelVersion(mlPrediction.modelVersion());
    entity.setDriftScore(java.math.BigDecimal.valueOf(driftMonitorService.currentDriftScore()));
    entity.setLatencyMs(
        mlPrediction.inferenceLatencyMs() > 0
            ? mlPrediction.inferenceLatencyMs()
            : assessment.latencyMs());
    entity.setFeatureSnapshot(write(featureSnapshot(event, assessment, mlPrediction)));
    repository.save(entity);
  }

  private Map<String, Object> featureSnapshot(
      TransactionEvent event, RiskAssessment assessment, MlFraudPrediction mlPrediction) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("transactionId", event.transactionId());
    snapshot.put("customerId", event.customerId());
    snapshot.put("modelType", mlPrediction.modelType());
    snapshot.put("modelVersion", mlPrediction.modelVersion());
    snapshot.put("fraudProbability", mlPrediction.fraudProbability());
    snapshot.put("mlScore", assessment.scoreBreakdown().mlScore());
    snapshot.put("ruleScore", assessment.scoreBreakdown().ruleScore());
    snapshot.put("twinScore", assessment.scoreBreakdown().twinDeviationScore());
    snapshot.put("topFeatures", mlPrediction.topFeatures());
    snapshot.put("featureVector", mlPrediction.featureVector());
    snapshot.put("inferenceLatencyMs", mlPrediction.inferenceLatencyMs());
    return snapshot;
  }

  private String write(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception ex) {
      return "{}";
    }
  }
}
