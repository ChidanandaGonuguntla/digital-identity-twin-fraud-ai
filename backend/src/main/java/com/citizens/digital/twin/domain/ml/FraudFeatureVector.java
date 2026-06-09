package com.citizens.digital.twin.domain.ml;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record FraudFeatureVector(
    List<String> featureOrder, float[] values, Map<String, Double> namedValues) {

  public static final List<String> FEATURE_ORDER =
      List.of(
          "amount_log",
          "amount_zscore",
          "hour_of_day_norm",
          "is_new_device",
          "is_new_country",
          "is_high_risk_channel",
          "merchant_risk_score",
          "category_frequency",
          "twin_txn_count_norm",
          "geo_distance_norm");

  public static FraudFeatureVector of(Map<String, Double> named) {
    LinkedHashMap<String, Double> ordered = new LinkedHashMap<>();
    float[] values = new float[FEATURE_ORDER.size()];
    for (int i = 0; i < FEATURE_ORDER.size(); i++) {
      String key = FEATURE_ORDER.get(i);
      double value = named.getOrDefault(key, 0.0);
      ordered.put(key, value);
      values[i] = (float) value;
    }
    return new FraudFeatureVector(FEATURE_ORDER, values, ordered);
  }
}
