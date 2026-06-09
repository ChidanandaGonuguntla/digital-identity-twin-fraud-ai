package com.citizens.digital.twin.api.dto;

import java.time.Instant;
import java.util.List;

public record FraudCaseResponse(
    String caseId,
    String assessmentId,
    String transactionId,
    String customerId,
    String status,
    String priority,
    String assignedTo,
    Instant slaDueAt,
    int escalationLevel,
    String closureReason,
    String summary,
    Instant createdAt,
    Instant updatedAt,
    Instant closedAt,
    List<FraudCaseEventResponse> events) {}
