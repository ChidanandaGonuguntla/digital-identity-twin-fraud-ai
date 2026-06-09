package com.citizens.digital.twin.domain.ml.onnx;

import com.citizens.digital.twin.domain.ml.FraudFeatureVector;
import com.citizens.digital.twin.domain.ml.MlFraudModelService;
import com.citizens.digital.twin.domain.ml.MlFraudPrediction;
import com.citizens.digital.twin.domain.ml.ModelMetadata;
import com.citizens.digital.twin.domain.ml.feature.FraudFeatureEngineeringService;
import com.citizens.digital.twin.domain.model.IdentityTwin;
import com.citizens.digital.twin.domain.model.TransactionEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OnnxFraudModelService implements MlFraudModelService {
  private final OnnxModelSessionManager sessionManager;
  private final FraudFeatureEngineeringService featureEngineeringService;

  public OnnxFraudModelService(
      OnnxModelSessionManager sessionManager,
      FraudFeatureEngineeringService featureEngineeringService) {
    this.sessionManager = sessionManager;
    this.featureEngineeringService = featureEngineeringService;
  }

  public boolean isReady() {
    return sessionManager.isReady();
  }

  @Override
  public MlFraudPrediction predict(IdentityTwin twin, TransactionEvent event) {
    if (!sessionManager.isReady()) {
      throw new IllegalStateException("ONNX model is not loaded");
    }
    long startedAt = System.nanoTime();
    FraudFeatureVector features = featureEngineeringService.build(twin, event);
    try {
      double probability = sessionManager.predict(features.values());
      long latencyMs = (System.nanoTime() - startedAt) / 1_000_000L;
      OnnxModelSessionManager.LoadedModel loaded = sessionManager.currentModel().orElseThrow();
      BigDecimal fraudProbability =
          BigDecimal.valueOf(probability)
              .min(BigDecimal.ONE)
              .max(BigDecimal.ZERO)
              .setScale(4, RoundingMode.HALF_UP);
      return new MlFraudPrediction(
          fraudProbability,
          fraudProbability.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP),
          loaded.artifact().modelVersion(),
          loaded.artifact().modelType(),
          topFeatures(features.namedValues()),
          features.namedValues(),
          latencyMs);
    } catch (Exception ex) {
      throw new IllegalStateException("ONNX inference failed", ex);
    }
  }

  public MlFraudPrediction predictChallenger(IdentityTwin twin, TransactionEvent event) {
    if (!sessionManager.isChallengerReady()) {
      return null;
    }
    FraudFeatureVector features = featureEngineeringService.build(twin, event);
    try {
      double probability = sessionManager.predictChallenger(features.values());
      OnnxModelSessionManager.LoadedModel loaded =
          sessionManager.currentChallengerModel().orElseThrow();
      BigDecimal fraudProbability =
          BigDecimal.valueOf(probability)
              .min(BigDecimal.ONE)
              .max(BigDecimal.ZERO)
              .setScale(4, RoundingMode.HALF_UP);
      return new MlFraudPrediction(
          fraudProbability,
          fraudProbability.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP),
          loaded.artifact().modelVersion(),
          loaded.artifact().modelType(),
          topFeatures(features.namedValues()),
          features.namedValues(),
          0L);
    } catch (Exception ex) {
      return null;
    }
  }

  @Override
  public ModelMetadata metadata() {
    return sessionManager
        .currentModel()
        .map(
            loaded -> {
              Map<String, Double> metrics = loaded.artifact().metrics();
              return new ModelMetadata(
                  loaded.artifact().modelName(),
                  loaded.artifact().modelVersion(),
                  loaded.artifact().modelType(),
                  loaded.artifact().trainingDatasetVersion(),
                  loaded.artifact().featureSchemaVersion(),
                  "ACTIVE",
                  loaded.artifact().trainedAt(),
                  Instant.now(),
                  metrics.getOrDefault("auc", 0.0),
                  metrics.getOrDefault("precision", 0.0),
                  metrics.getOrDefault("recall", 0.0),
                  metrics.getOrDefault("f1Score", 0.0),
                  metrics.getOrDefault("falsePositiveRate", 0.0),
                  loaded.artifact().baselineMeanScore(),
                  null,
                  null);
            })
        .orElse(
            new ModelMetadata(
                "digital-twin-fraud-risk",
                "unloaded",
                "xgboost-onnx",
                "fraud-synthetic-v1.0.0",
                "fraud-features-v1.0.0",
                "INACTIVE",
                Instant.EPOCH,
                Instant.now(),
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                null,
                null));
  }

  private List<String> topFeatures(Map<String, Double> featureVector) {
    List<String> ranked = new ArrayList<>();
    featureVector.entrySet().stream()
        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
        .limit(4)
        .forEach(
            entry -> ranked.add(entry.getKey() + "=" + String.format("%.2f", entry.getValue())));
    return ranked;
  }
}
