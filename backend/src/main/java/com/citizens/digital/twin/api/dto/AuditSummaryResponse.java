package com.citizens.digital.twin.api.dto;

public record AuditSummaryResponse(
    long totalEvents,
    long blocked,
    long reviews,
    long allowed,
    double averageRiskScore,
    double blockRate,
    long p95LatencyMs,
    double preventedAmount) {}
