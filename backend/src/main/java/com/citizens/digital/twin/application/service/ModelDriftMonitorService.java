package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.domain.ml.MlFraudPrediction;
import com.citizens.digital.twin.domain.ml.onnx.OnnxModelSessionManager;
import com.citizens.digital.twin.infrastructure.config.MlModelProperties;
import com.citizens.digital.twin.infrastructure.persistence.repository.ModelMetricsJpaRepository;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;

@Service
public class ModelDriftMonitorService {
  private final MlModelProperties properties;
  private final ModelMetricsJpaRepository repository;
  private final OnnxModelSessionManager sessionManager;
  private final AtomicReference<Double> lastHourMean = new AtomicReference<>(0.0);

  public ModelDriftMonitorService(
      MlModelProperties properties,
      ModelMetricsJpaRepository repository,
      OnnxModelSessionManager sessionManager) {
    this.properties = properties;
    this.repository = repository;
    this.sessionManager = sessionManager;
  }

  public void record(MlFraudPrediction prediction) {
    lastHourMean.set(prediction.fraudProbability().doubleValue());
  }

  public double currentDriftScore() {
    Double dbMean = repository.averageMlScoreLastHour();
    double current = dbMean == null ? lastHourMean.get() : dbMean;
    double baseline = resolveBaseline();
    if (baseline <= 0.0) {
      return 0.0;
    }
    return Math.abs(current - baseline) / baseline;
  }

  public boolean driftAlert() {
    return currentDriftScore() >= properties.driftAlertThreshold();
  }

  public double baselineScore() {
    return resolveBaseline();
  }

  private double resolveBaseline() {
    return sessionManager
        .currentModel()
        .map(model -> model.artifact().baselineMeanScore())
        .filter(score -> score > 0.0)
        .orElse(properties.driftBaselineScore());
  }
}
