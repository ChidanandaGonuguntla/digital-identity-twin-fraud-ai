package com.citizens.digital.twin.api.dto;

import java.time.Instant;
import java.util.Map;

public record Customer360TimelineEvent(
    String eventType,
    String eventId,
    String title,
    String description,
    Instant occurredAt,
    Map<String, Object> metadata) {}
