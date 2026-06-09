package com.citizens.digital.twin.domain.ml;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record MlFraudPrediction(
    BigDecimal fraudProbability,
    BigDecimal score,
    String modelVersion,
    String modelType,
    List<String> topFeatures,
    Map<String, Double> featureVector,
    long inferenceLatencyMs) {

  public MlFraudPrediction(
      BigDecimal fraudProbability,
      BigDecimal score,
      String modelVersion,
      List<String> topFeatures) {
    this(fraudProbability, score, modelVersion, "heuristic", topFeatures, Map.of(), 0L);
  }
}
