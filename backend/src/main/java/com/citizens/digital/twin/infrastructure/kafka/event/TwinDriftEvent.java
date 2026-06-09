package com.citizens.digital.twin.infrastructure.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;

public record TwinDriftEvent(
    String driftEventId,
    String assessmentId,
    String customerId,
    String transactionId,
    BigDecimal driftScore,
    BigDecimal driftThreshold,
    Instant detectedAt) {}
