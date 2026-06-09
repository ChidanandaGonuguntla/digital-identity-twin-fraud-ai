package com.citizens.digital.twin.api.dto;

import java.time.Instant;

public record PlatformOpsSummaryResponse(
    long p95LatencyMs,
    long kafkaConsumerLag,
    double driftScore,
    boolean driftAlert,
    String serviceStatus,
    long scoredLastHour,
    boolean sloLatencyMet,
    boolean sloKafkaLagMet,
    boolean sloDriftMet,
    Instant evaluatedAt) {}
