package com.citizens.digital.twin.api.dto;

public record ModelQualityMetricsResponse(
    long labeledSamples,
    long truePositives,
    long falsePositives,
    long falseNegatives,
    long trueNegatives,
    double precision,
    double recall,
    double auc,
    double f1Score,
    double falsePositiveRate) {}
