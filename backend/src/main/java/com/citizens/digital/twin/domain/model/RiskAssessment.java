package com.citizens.digital.twin.domain.model;

import java.time.Instant;
import java.util.List;

public record RiskAssessment(
    String assessmentId,
    String transactionId,
    String customerId,
    Decision decision,
    ScoreBreakdown scoreBreakdown,
    List<RiskSignal> reasonCodes,
    String modelVersion,
    String policyVersion,
    long latencyMs,
    Instant assessedAt) {

  public boolean isBlocked() {
    return decision == Decision.BLOCK;
  }

  public boolean requiresChallenge() {
    return decision == Decision.CHALLENGE;
  }

  public boolean isAllowed() {
    return decision == Decision.ALLOW;
  }

  public double riskScore() {
    return scoreBreakdown == null || scoreBreakdown.finalScore() == null
        ? 0.0
        : scoreBreakdown.finalScore().doubleValue();
  }
}
