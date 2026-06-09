package com.citizens.digital.twin.api.dto;

import java.time.Instant;

public record AnalystFeedbackResponse(
    String feedbackId,
    String assessmentId,
    String transactionId,
    String customerId,
    String outcome,
    String analystId,
    String notes,
    Instant createdAt) {}
