package com.citizens.digital.twin.api.dto;

import java.time.Instant;

public record FraudCaseSummaryResponse(
    String caseId,
    String assessmentId,
    String transactionId,
    String customerId,
    String status,
    String priority,
    String assignedTo,
    Instant slaDueAt,
    int escalationLevel,
    String summary,
    Instant createdAt,
    Instant updatedAt) {}
