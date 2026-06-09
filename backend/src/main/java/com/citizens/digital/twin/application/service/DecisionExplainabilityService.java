package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.api.dto.DecisionNarrativeResponse;
import com.citizens.digital.twin.api.dto.ExplainabilityFactorResponse;
import com.citizens.digital.twin.api.dto.FeatureContributionResponse;
import com.citizens.digital.twin.api.dto.FraudDecisionAuditDetailResponse;
import com.citizens.digital.twin.api.dto.ScoreAttributionResponse;
import com.citizens.digital.twin.api.dto.ShapFeatureResponse;
import com.citizens.digital.twin.domain.model.RiskSignal;
import com.citizens.digital.twin.domain.model.RiskSignalType;
import com.citizens.digital.twin.domain.model.ScoreBreakdown;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class DecisionExplainabilityService {
  private static final double RULE_WEIGHT = 0.35;
  private static final double TWIN_WEIGHT = 0.35;
  private static final double ML_WEIGHT = 0.30;

  private static final Map<String, Double> FEATURE_IMPORTANCE =
      Map.ofEntries(
          Map.entry("is_new_device", 0.19),
          Map.entry("is_new_country", 0.17),
          Map.entry("amount_zscore", 0.15),
          Map.entry("geo_distance_norm", 0.12),
          Map.entry("merchant_risk_score", 0.11),
          Map.entry("is_high_risk_channel", 0.08),
          Map.entry("category_frequency", 0.07),
          Map.entry("amount_log", 0.05),
          Map.entry("hour_of_day_norm", 0.04),
          Map.entry("twin_txn_count_norm", 0.02));

  private static final Map<String, String> FEATURE_LABELS =
      Map.ofEntries(
          Map.entry("amount_log", "Transaction amount (log)"),
          Map.entry("amount_zscore", "Amount vs customer baseline"),
          Map.entry("hour_of_day_norm", "Unusual hour of day"),
          Map.entry("is_new_device", "New device"),
          Map.entry("is_new_country", "Foreign country"),
          Map.entry("is_high_risk_channel", "High-risk channel"),
          Map.entry("merchant_risk_score", "Merchant risk"),
          Map.entry("category_frequency", "Rare merchant category"),
          Map.entry("twin_txn_count_norm", "Limited twin history"),
          Map.entry("geo_distance_norm", "Geo distance from last txn"));

  private static final Pattern AMOUNT_BASELINE_PATTERN =
      Pattern.compile("amount=([0-9.\\-]+).*mean=([0-9.\\-]+)");

  private final FraudDecisionAuditService auditService;

  public DecisionExplainabilityService(FraudDecisionAuditService auditService) {
    this.auditService = auditService;
  }

  public DecisionNarrativeResponse build(String assessmentId) {
    FraudDecisionAuditDetailResponse detail = auditService.getDetail(assessmentId);
    ScoreAttributionResponse attribution = buildAttribution(detail);
    List<ShapFeatureResponse> shapFeatures = buildShapFeatures(detail, attribution.mlProbability());
    List<ExplainabilityFactorResponse> factors = buildFactors(detail, attribution);
    List<FeatureContributionResponse> topFeatures =
        shapFeatures.stream()
            .sorted(Comparator.comparingDouble(ShapFeatureResponse::shapValue).reversed())
            .limit(8)
            .map(
                feature ->
                    new FeatureContributionResponse(
                        feature.feature(), round3(feature.value()), round3(feature.shapValue())))
            .toList();
    List<String> bullets =
        factors.stream()
            .map(factor -> factor.label() + " contributed " + round1(factor.points()) + " points")
            .toList();
    if (attribution.mlProbability() > 0) {
      bullets.add(
          "ML model assigned " + round1(attribution.mlProbability()) + "% fraud probability");
    }
    String headline = headlineFor(detail.decision());
    String narrative = headline + " " + String.join("; ", bullets.stream().limit(4).toList());
    return new DecisionNarrativeResponse(
        detail.assessmentId(),
        detail.decision(),
        detail.finalScore(),
        headline,
        narrative,
        bullets,
        factors,
        attribution,
        shapFeatures,
        topFeatures,
        attribution.mlProbability(),
        detail.championScore(),
        detail.challengerScore(),
        detail.scoreDelta(),
        detail.modelAgreement());
  }

  private ScoreAttributionResponse buildAttribution(FraudDecisionAuditDetailResponse detail) {
    ScoreBreakdown breakdown = detail.scoreBreakdown();
    double rule = breakdown.ruleScore().doubleValue();
    double twin = breakdown.twinDeviationScore().doubleValue();
    double ml = breakdown.mlScore().doubleValue();
    double graph = breakdown.graphScore().doubleValue();
    double mlProbability =
        detail.championScore() != null
            ? detail.championScore()
            : Math.min(100.0, Math.max(0.0, ml));
    return new ScoreAttributionResponse(
        round1(rule * RULE_WEIGHT),
        round1(twin * TWIN_WEIGHT),
        round1(ml * ML_WEIGHT),
        round1(graph),
        round1(breakdown.finalScore().doubleValue()),
        round1(mlProbability));
  }

  private List<ShapFeatureResponse> buildShapFeatures(
      FraudDecisionAuditDetailResponse detail, double mlProbability) {
    Map<String, Double> vector = detail.featureVector();
    if (vector == null || vector.isEmpty()) {
      return List.of();
    }
    Map<String, Double> weighted = new LinkedHashMap<>();
    double totalWeight = 0.0;
    for (Map.Entry<String, Double> entry : vector.entrySet()) {
      double importance = FEATURE_IMPORTANCE.getOrDefault(entry.getKey(), 0.05);
      double centered = entry.getValue() - 0.5;
      double raw = centered * importance;
      weighted.put(entry.getKey(), raw);
      totalWeight += Math.abs(raw);
    }
    if (totalWeight <= 0.0001) {
      totalWeight = 1.0;
    }
    List<ShapFeatureResponse> features = new ArrayList<>();
    for (Map.Entry<String, Double> entry : weighted.entrySet()) {
      double shap = (entry.getValue() / totalWeight) * mlProbability;
      features.add(
          new ShapFeatureResponse(
              entry.getKey(),
              FEATURE_LABELS.getOrDefault(entry.getKey(), formatFeatureName(entry.getKey())),
              round3(vector.get(entry.getKey())),
              round3(shap),
              shap >= 0 ? "INCREASES_FRAUD" : "DECREASES_FRAUD"));
    }
    features.sort(Comparator.comparingDouble(ShapFeatureResponse::shapValue).reversed());
    return features.stream().limit(10).toList();
  }

  private List<ExplainabilityFactorResponse> buildFactors(
      FraudDecisionAuditDetailResponse detail, ScoreAttributionResponse attribution) {
    List<ExplainabilityFactorResponse> factors = new ArrayList<>();
    for (RiskSignal signal :
        detail.reasonCodes().stream()
            .sorted(
                Comparator.comparingDouble(
                        (RiskSignal signal) -> signal.scoreContribution().doubleValue())
                    .reversed())
            .toList()) {
      if (signal.scoreContribution().doubleValue() <= 0
          && !RiskSignalType.ML_MODEL.equals(signal.type())) {
        continue;
      }
      factors.add(mapSignal(signal, attribution.mlProbability()));
    }
    if (factors.stream().noneMatch(f -> "ML".equals(f.source()))
        && attribution.mlProbability() > 0) {
      factors.add(
          new ExplainabilityFactorResponse(
              "ML model",
              "Assigned " + round1(attribution.mlProbability()) + "% fraud probability",
              round1(attribution.mlPoints()),
              "ML"));
    }
    return factors.stream().limit(8).toList();
  }

  private ExplainabilityFactorResponse mapSignal(RiskSignal signal, double mlProbability) {
    String source =
        switch (signal.type()) {
          case RULE -> "RULE";
          case TWIN_DEVIATION -> "TWIN";
          case ML_MODEL -> "ML";
          default -> "POLICY";
        };
    double points = signal.scoreContribution().doubleValue();
    if (RiskSignalType.ML_MODEL.equals(signal.type()) && points <= 0) {
      points = mlProbability * ML_WEIGHT;
    }
    return new ExplainabilityFactorResponse(
        factorLabel(signal), factorDetail(signal), round1(points), source);
  }

  private String factorLabel(RiskSignal signal) {
    return switch (signal.code()) {
      case "NEW_DEVICE" -> "New device";
      case "NEW_COUNTRY" -> "Foreign country";
      case "AMOUNT_ANOMALY_CRITICAL", "AMOUNT_ANOMALY_ELEVATED" -> "Amount vs baseline";
      case "IMPOSSIBLE_TRAVEL", "FAST_TRAVEL", "SIMULTANEOUS_GEO_DISTANCE" -> "Geo velocity";
      case "HIGH_RISK_CATEGORY" -> "High-risk merchant category";
      case "NEW_MERCHANT_CATEGORY" -> "New merchant category";
      case "CATEGORY_VELOCITY" -> "Category velocity";
      case "UNUSUAL_HOUR" -> "Unusual hour";
      case "HIGH_VALUE_TRANSACTION" -> "High-value transaction";
      case "BLOCKED_COUNTRY" -> "Blocked country";
      case "HIGH_RISK_CHANNEL" -> "High-risk channel";
      case "ML_HIGH_FRAUD_PROBABILITY" -> "ML model";
      default -> signal.message();
    };
  }

  private String factorDetail(RiskSignal signal) {
    if (signal.code().startsWith("AMOUNT_ANOMALY")) {
      Matcher matcher =
          AMOUNT_BASELINE_PATTERN.matcher(signal.evidence() == null ? "" : signal.evidence());
      if (matcher.find()) {
        double amount = Double.parseDouble(matcher.group(1));
        double mean = Double.parseDouble(matcher.group(2));
        if (mean > 0 && amount > mean) {
          return "Amount was " + round1(amount / mean) + "x higher than customer baseline";
        }
      }
    }
    if ("ML_HIGH_FRAUD_PROBABILITY".equals(signal.code())) {
      return signal.message();
    }
    if (signal.evidence() != null && !signal.evidence().isBlank()) {
      return signal.message() + " (" + signal.evidence() + ")";
    }
    return signal.message();
  }

  private String headlineFor(String decision) {
    return switch (decision == null ? "" : decision.toUpperCase(Locale.ROOT)) {
      case "BLOCK" -> "Blocked because:";
      case "CHALLENGE" -> "Challenged because:";
      case "ALLOW" -> "Allowed because:";
      default -> decision + " because:";
    };
  }

  private String formatFeatureName(String feature) {
    return feature.replace('_', ' ');
  }

  private double round1(double value) {
    return Math.round(value * 10.0) / 10.0;
  }

  private double round3(double value) {
    return Math.round(value * 1000.0) / 1000.0;
  }
}
