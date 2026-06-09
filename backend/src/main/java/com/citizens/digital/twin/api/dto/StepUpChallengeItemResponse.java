package com.citizens.digital.twin.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record StepUpChallengeItemResponse(
    String challengeId,
    String assessmentId,
    String customerId,
    String transactionId,
    String status,
    String deliveryChannel,
    String reasonDescription,
    BigDecimal finalRiskScore,
    Instant expiresAt,
    Instant createdAt) {}
