package com.citizens.digital.twin.api.dto;

import java.time.Instant;

public record ModelRegistryEntryResponse(
    String modelName,
    String modelVersion,
    String trainingDatasetVersion,
    String featureSchemaVersion,
    String status,
    boolean active,
    double auc,
    double precision,
    double recall,
    double f1Score,
    double falsePositiveRate,
    double driftScore,
    String approvedBy,
    Instant approvedAt,
    Instant trainedAt,
    Instant deployedAt,
    String rejectionReason) {}
