package com.citizens.digital.twin.api.dto;

import com.citizens.digital.twin.domain.model.RiskAssessment;
import com.citizens.digital.twin.domain.model.RiskSignal;
import com.citizens.digital.twin.domain.model.ScoreBreakdown;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record FraudDecisionResponse(
    String assessmentId,
    String transactionId,
    String customerId,
    String decision,
    BigDecimal finalScore,
    ScoreBreakdown scoreBreakdown,
    List<RiskSignal> reasonCodes,
    String modelVersion,
    String policyVersion,
    long latencyMs,
    Instant assessedAt,
    DecisionEvent event) {
  public static FraudDecisionResponse from(RiskAssessment a, DecisionEvent e) {
    return new FraudDecisionResponse(
        a.assessmentId(),
        a.transactionId(),
        a.customerId(),
        a.decision().name(),
        a.scoreBreakdown().finalScore(),
        a.scoreBreakdown(),
        a.reasonCodes(),
        a.modelVersion(),
        a.policyVersion(),
        a.latencyMs(),
        a.assessedAt(),
        e);
  }
}
