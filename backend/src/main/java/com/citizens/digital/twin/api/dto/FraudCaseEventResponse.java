package com.citizens.digital.twin.api.dto;

import java.time.Instant;
import java.util.Map;

public record FraudCaseEventResponse(
    String eventId,
    String eventType,
    String actorId,
    Map<String, Object> payload,
    Instant createdAt) {}
