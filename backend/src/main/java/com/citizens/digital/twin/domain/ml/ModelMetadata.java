package com.citizens.digital.twin.domain.ml;

import java.time.Instant;

public record ModelMetadata(
    String modelName,
    String modelVersion,
    String modelType,
    String trainingDatasetVersion,
    String featureSchemaVersion,
    String status,
    Instant trainedAt,
    Instant loadedAt,
    double auc,
    double precision,
    double recall,
    double f1Score,
    double falsePositiveRate,
    double driftScore,
    String approvedBy,
    Instant approvedAt) {}
