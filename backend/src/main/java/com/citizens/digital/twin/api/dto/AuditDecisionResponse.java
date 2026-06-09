package com.citizens.digital.twin.api.dto;

import java.time.Instant;
import java.util.List;

public record AuditDecisionResponse(
    String assessmentId,
    String transactionId,
    String customerId,
    String decision,
    double riskScore,
    double amount,
    String merchantCategory,
    String deviceId,
    double latitude,
    double longitude,
    long latencyMs,
    Instant assessedAt,
    List<String> reasons,
    boolean coldStart,
    String modelVersion,
    String policyVersion,
    String featureVersion,
    String finalDecisionReason,
    boolean challenged,
    boolean twinUpdated) {}
