package com.citizens.digital.twin.api.dto;

public record FeatureDriftMetricResponse(
    String featureName,
    double baselineMean,
    double currentMean,
    double driftIndex,
    String severity) {}
