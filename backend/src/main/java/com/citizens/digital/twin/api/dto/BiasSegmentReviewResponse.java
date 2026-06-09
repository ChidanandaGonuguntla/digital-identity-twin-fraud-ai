package com.citizens.digital.twin.api.dto;

public record BiasSegmentReviewResponse(
    String segment,
    long sampleSize,
    double blockRate,
    double falsePositiveRate,
    String fairnessStatus) {}
