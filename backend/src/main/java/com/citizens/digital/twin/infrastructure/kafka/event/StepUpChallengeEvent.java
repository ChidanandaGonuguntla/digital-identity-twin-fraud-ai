package com.citizens.digital.twin.infrastructure.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;

public record StepUpChallengeEvent(
    String challengeId,
    String assessmentId,
    String customerId,
    String transactionId,
    String challengeStatus,
    String deliveryChannel,
    String reasonDescription,
    BigDecimal finalRiskScore,
    Instant expiresAt,
    Instant createdAt) {}
