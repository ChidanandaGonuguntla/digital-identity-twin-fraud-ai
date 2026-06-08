package com.citizens.dti.web;

import java.util.List;

/**
 * The wire contract broadcast to the SENTINEL console over /ws/decisions.
 *
 * <p>It merges the original {@code TransactionEvent} (amount, location, device, ...) with the
 * {@code RiskAssessment} (score, decision, reasons) so the UI can render a fully-decided
 * transaction from a single message. Field names and types match the "WebSocket contract"
 * documented in the console README exactly.
 *
 * @param timestamp epoch MILLISECONDS (the console treats the timestamp as ms)
 * @param decision one of ALLOW | CHALLENGE | BLOCK
 */
public record DecisionEvent(
    String transactionId,
    String customerId,
    double amount,
    String merchantCategory,
    String deviceId,
    double latitude,
    double longitude,
    long timestamp,
    double riskScore,
    String decision,
    boolean coldStart,
    List<SignalContribution> signals,
    List<String> reasons) {}
