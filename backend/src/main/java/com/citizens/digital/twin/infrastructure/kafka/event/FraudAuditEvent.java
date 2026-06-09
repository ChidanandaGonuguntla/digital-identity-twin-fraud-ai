package com.citizens.digital.twin.infrastructure.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;

public record FraudAuditEvent(
    String assessmentId,
    String transactionId,
    String customerId,
    String decision,
    BigDecimal finalScore,
    String modelVersion,
    String policyVersion,
    long latencyMs,
    Instant assessedAt) {}
