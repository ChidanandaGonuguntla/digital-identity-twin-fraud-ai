package com.citizens.digital.twin.application.orchestrator;

import com.citizens.digital.twin.domain.ml.MlFraudPrediction;
import com.citizens.digital.twin.domain.model.*;
import com.citizens.digital.twin.domain.policy.FraudPolicyConstants;
import com.citizens.digital.twin.domain.rule.RuleRiskEngine.RuleScore;
import com.citizens.digital.twin.domain.scoring.TwinDeviationScoringService.TwinDeviationScore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RiskDecisionOrchestrator {

  private static final BigDecimal RULE_WEIGHT = BigDecimal.valueOf(0.35);
  private static final BigDecimal TWIN_WEIGHT = BigDecimal.valueOf(0.35);
  private static final BigDecimal ML_WEIGHT = BigDecimal.valueOf(0.30);
  private static final BigDecimal BLOCK_THRESHOLD = BigDecimal.valueOf(75);
  private static final BigDecimal CHALLENGE_THRESHOLD = BigDecimal.valueOf(45);
  private static final BigDecimal MAX_SCORE = BigDecimal.valueOf(100);

  public RiskAssessment decide(
      TransactionEvent event,
      IdentityTwin twin,
      RuleScore ruleScore,
      TwinDeviationScore twinScore,
      MlFraudPrediction mlPrediction,
      long latencyMs) {
    BigDecimal rule = safe(ruleScore.score());
    BigDecimal twinDeviation = safe(twinScore.score());
    BigDecimal ml = safe(mlPrediction.score());
    BigDecimal finalScore = weightedScore(rule, twinDeviation, ml);
    Decision decision = decisionFromScore(finalScore);
    List<RiskSignal> reasonCodes = new ArrayList<>();
    reasonCodes.addAll(ruleScore.signals());
    reasonCodes.addAll(twinScore.signals());
    if (ml.compareTo(BigDecimal.valueOf(65)) >= 0)
      reasonCodes.add(
          new RiskSignal(
              RiskSignalType.ML_MODEL,
              "ML_HIGH_FRAUD_PROBABILITY",
              "ML fraud model returned elevated fraud probability.",
              Severity.HIGH,
              ml.multiply(ML_WEIGHT).setScale(2, RoundingMode.HALF_UP),
              "fraudProbability="
                  + mlPrediction.fraudProbability()
                  + ", topFeatures="
                  + mlPrediction.topFeatures()));
    if (reasonCodes.stream()
        .anyMatch(s -> "BLOCKED_COUNTRY".equals(s.code()) || s.severity() == Severity.CRITICAL))
      decision = Decision.BLOCK;
    if (reasonCodes.isEmpty())
      reasonCodes.add(
          new RiskSignal(
              RiskSignalType.POLICY,
              "NO_MATERIAL_RISK_SIGNAL",
              "No material fraud deviation detected.",
              Severity.LOW,
              BigDecimal.ZERO,
              "All scoring engines below risk threshold"));
    return new RiskAssessment(
        UUID.randomUUID().toString(),
        event.transactionId(),
        event.customerId(),
        decision,
        new ScoreBreakdown(rule, twinDeviation, ml, BigDecimal.ZERO, finalScore),
        reasonCodes,
        mlPrediction.modelVersion(),
        FraudPolicyConstants.POLICY_VERSION,
        latencyMs,
        Instant.now());
  }

  private BigDecimal weightedScore(BigDecimal rule, BigDecimal twin, BigDecimal ml) {
    return rule.multiply(RULE_WEIGHT)
        .add(twin.multiply(TWIN_WEIGHT))
        .add(ml.multiply(ML_WEIGHT))
        .setScale(2, RoundingMode.HALF_UP)
        .min(MAX_SCORE);
  }

  private Decision decisionFromScore(BigDecimal finalScore) {
    if (finalScore.compareTo(BLOCK_THRESHOLD) >= 0) return Decision.BLOCK;
    if (finalScore.compareTo(CHALLENGE_THRESHOLD) >= 0) return Decision.CHALLENGE;
    return Decision.ALLOW;
  }

  private BigDecimal safe(BigDecimal score) {
    if (score == null) return BigDecimal.ZERO;
    if (score.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
    if (score.compareTo(MAX_SCORE) > 0) return MAX_SCORE;
    return score.setScale(2, RoundingMode.HALF_UP);
  }
}
