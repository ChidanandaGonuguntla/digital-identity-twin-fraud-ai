package com.citizens.digital.twin.api.dto;

import java.time.Instant;
import java.util.List;

public record BiasFairnessReviewResponse(
    String reviewStatus,
    Instant lastReviewedAt,
    String modelVersion,
    List<BiasSegmentReviewResponse> segments,
    String notes) {}
