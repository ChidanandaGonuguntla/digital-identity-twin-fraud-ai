package com.citizens.digital.twin.api.dto;

import java.time.Instant;

public record ModelDriftPointResponse(
    Instant bucket,
    long scored,
    double avgRisk,
    double riskSpread,
    double driftScore,
    double avgLatencyMs) {}
