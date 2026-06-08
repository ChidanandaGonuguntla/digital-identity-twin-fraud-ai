package com.citizens.dti.model;

import java.time.Instant;
import java.util.List;

/**
 * Explainable result of scoring an event against the twin. The {@code reasons} list is deliberately
 * human-readable: in banking, fraud decisions must be auditable and explainable to analysts and
 * regulators, not just a black-box score.
 */
public record RiskAssessment(
    String transactionId,
    String customerId,
    double riskScore,
    Decision decision,
    List<String> reasons,
    boolean coldStart,
    Instant assessedAt) {}
