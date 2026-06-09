package com.citizens.digital.twin.api.dto;

import java.time.Instant;
import java.util.List;

public record DataDriftSummaryResponse(
    String modelVersion,
    String featureSchemaVersion,
    double modelDriftScore,
    boolean driftAlert,
    Instant evaluatedAt,
    List<FeatureDriftMetricResponse> features) {}
