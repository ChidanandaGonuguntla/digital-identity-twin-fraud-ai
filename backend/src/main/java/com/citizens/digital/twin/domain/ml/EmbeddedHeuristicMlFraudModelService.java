package com.citizens.digital.twin.domain.ml;

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
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class EmbeddedHeuristicMlFraudModelService implements MlFraudModelService {
  private final FraudFeatureEngineeringService featureEngineeringService;

  public EmbeddedHeuristicMlFraudModelService(
      FraudFeatureEngineeringService featureEngineeringService) {
    this.featureEngineeringService = featureEngineeringService;
  }

  @Override
  public MlFraudPrediction predict(IdentityTwin twin, TransactionEvent event) {
    Objects.requireNonNull(event, "transaction event must not be null");
    var featureVector = featureEngineeringService.build(twin, event);
    BigDecimal score = BigDecimal.ZERO;
    List<String> features = new ArrayList<>();
    score =
        score.add(
            weight(
                featureVector.namedValues().getOrDefault("amount_zscore", 0.0),
                0.30,
                "amount_zscore",
                features));
    score =
        score.add(
            weight(
                featureVector.namedValues().getOrDefault("is_new_device", 0.0),
                0.25,
                "is_new_device",
                features));
    score =
        score.add(
            weight(
                featureVector.namedValues().getOrDefault("is_new_country", 0.0),
                0.35,
                "is_new_country",
                features));
    score =
        score.add(
            weight(
                featureVector.namedValues().getOrDefault("is_high_risk_channel", 0.0),
                0.15,
                "is_high_risk_channel",
                features));
    score =
        score.add(
            weight(
                featureVector.namedValues().getOrDefault("merchant_risk_score", 0.0),
                0.20,
                "merchant_risk_score",
                features));
    BigDecimal probability = score.min(BigDecimal.valueOf(0.99)).setScale(4, RoundingMode.HALF_UP);
    return new MlFraudPrediction(
        probability,
        probability.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP),
        metadata().modelVersion(),
        "heuristic-baseline",
        topFeatures(featureVector.namedValues()),
        featureVector.namedValues(),
        0L);
  }

  private BigDecimal weight(
      double signal, double contribution, String name, List<String> features) {
    if (signal <= 0.2) {
      return BigDecimal.ZERO;
    }
    features.add(name);
    return BigDecimal.valueOf(contribution * signal);
  }

  private List<String> topFeatures(Map<String, Double> featureVector) {
    List<String> ranked = new ArrayList<>();
    featureVector.entrySet().stream()
        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
        .limit(4)
        .forEach(entry -> ranked.add(entry.getKey()));
    return ranked;
  }

  private boolean isNewDevice(IdentityTwin twin, TransactionEvent event) {
    return twin != null
        && twin.getProfile() != null
        && event.deviceId() != null
        && !event.deviceId().isBlank()
        && !twin.getProfile().isKnownDevice(event.deviceId());
  }

  private boolean isNewCountry(IdentityTwin twin, TransactionEvent event) {
    return twin != null
        && twin.getProfile() != null
        && !twin.usualCountries().isEmpty()
        && event.countryCode() != null
        && !event.countryCode().isBlank()
        && !twin.isUsualCountry(event.countryCode());
  }

  private boolean isHighRiskChannel(TransactionEvent event) {
    return event.channel() != null
        && List.of("TOR", "PROXY", "UNKNOWN").contains(event.channel().trim().toUpperCase());
  }

  @Override
  public ModelMetadata metadata() {
    return new ModelMetadata(
        "digital-twin-fraud-risk",
        "fraud-risk-v1.0.0-embedded",
        "heuristic-baseline-replace-with-xgboost-or-onnx",
        "fraud-synthetic-v1.0.0",
        "fraud-features-v1.0.0",
        "ACTIVE",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.now(),
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.04,
        null,
        null);
  }
}
