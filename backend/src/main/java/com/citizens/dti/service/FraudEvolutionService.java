package com.citizens.dti.service;

import com.citizens.dti.dto.FraudEvaluationRequest;
import com.citizens.dti.dto.FraudEvaluationResponse;
import com.citizens.dti.dto.RiskSignalDto;
import com.citizens.dti.enums.FraudDecision;
import com.citizens.dti.model.Decision;
import com.citizens.dti.model.RiskAssessment;
import com.citizens.dti.model.TransactionEvent;
import com.citizens.dti.repository.IdentityTwinRepository;
import com.citizens.dti.twin.DeviationScoringEngine;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FraudEvolutionService {

  private static final String MODEL_NAME = "identity-twin";
  private static final String MODEL_VERSION = "1.0";

  private final IdentityTwinRepository repository;
  private final DeviationScoringEngine scoringEngine;

  public FraudEvolutionService(IdentityTwinRepository repository, DeviationScoringEngine scoringEngine) {
    this.repository = repository;
    this.scoringEngine = scoringEngine;
  }

  public FraudEvaluationResponse evaluate(@Valid FraudEvaluationRequest request) {
    List<String> reasons = new ArrayList<>();

    // Reconstruct a TransactionEvent from the request for scoring
    TransactionEvent event = toTransactionEvent(request);

    return repository
        .find(request.customerId())
        .map(
            twin -> {
              RiskAssessment assessment = scoringEngine.score(twin, event);
              List<RiskSignalDto> riskSignals = toRiskSignals(assessment.reasons());
              BigDecimal ruleScore = BigDecimal.valueOf(assessment.riskScore());
              BigDecimal finalRiskScore = ruleScore; // twin score is the final score

              return new FraudEvaluationResponse(
                  request.fraudEventId(),
                  null, // mlScoreId — not applicable for twin-based scoring
                  request.twinId(),
                  request.customerId(),
                  request.accountId(),
                  request.transactionId(),
                  ruleScore,
                  null, // mlFraudProbability — not available
                  null, // mlScore — not available
                  finalRiskScore,
                  toFraudDecision(assessment.decision()),
                  MODEL_NAME,
                  MODEL_VERSION,
                  riskSignals,
                  null, // stepUpChallenge — not applicable
                  null, // inferenceLatencyMs — not tracked
                  OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
            })
        .orElseGet(
            () -> {
              reasons.add("No identity twin found for customer — cannot evaluate fraud evolution");
              List<RiskSignalDto> riskSignals = toRiskSignals(reasons);
              return new FraudEvaluationResponse(
                  request.fraudEventId(),
                  null,
                  request.twinId(),
                  request.customerId(),
                  request.accountId(),
                  request.transactionId(),
                  BigDecimal.ZERO,
                  null,
                  null,
                  BigDecimal.ZERO,
                  FraudDecision.APPROVE,
                  MODEL_NAME,
                  MODEL_VERSION,
                  riskSignals,
                  null,
                  null,
                  OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
            });
  }

  private TransactionEvent toTransactionEvent(FraudEvaluationRequest request) {
    return new TransactionEvent(
        request.customerId(),
        request.transactionId(),
        request.amount().doubleValue(),
        request.merchantCategory() != null ? request.merchantCategory() : "unknown",
        request.deviceId() != null ? request.deviceId() : "unknown",
        0.0, // latitude not available in evaluation request
        0.0, // longitude not available in evaluation request
        request.eventTime() != null ? request.eventTime().toInstant() : Instant.now());
  }

  private FraudDecision toFraudDecision(Decision decision) {
    return switch (decision) {
      case ALLOW -> FraudDecision.APPROVE;
      case CHALLENGE -> FraudDecision.STEP_UP_REQUIRED;
      case BLOCK -> FraudDecision.DECLINE;
    };
  }

  private List<RiskSignalDto> toRiskSignals(List<String> reasons) {
    if (reasons == null || reasons.isEmpty()) {
      return List.of();
    }
    List<RiskSignalDto> signals = new ArrayList<>();
    for (String reason : reasons) {
      String severity = "HIGH";
      if (reason.contains("consistent") || reason.contains("learning")) {
        severity = "LOW";
      } else if (reason.contains("elevated") || reason.contains("Unusually")) {
        severity = "MEDIUM";
      }
      signals.add(new RiskSignalDto(
          "TWIN_" + signals.size(),
          reason,
          severity,
          BigDecimal.ZERO, // contribution not computed at signal level
          reason));
    }
    return signals;
  }
}
