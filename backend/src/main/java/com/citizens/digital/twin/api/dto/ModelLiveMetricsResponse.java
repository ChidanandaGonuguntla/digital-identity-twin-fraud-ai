package com.citizens.digital.twin.api.dto;

import java.time.Instant;

public record ModelLiveMetricsResponse(
    long scoredLastHour,
    long scoredLast24Hours,
    Instant lastScoredAt,
    double avgLatencyMs,
    double avgRiskScoreLastHour,
    double avgMlScoreLastHour,
    double avgRuleScoreLastHour,
    double avgTwinScoreLastHour,
    double blockRateLastHour,
    double driftScore) {}
