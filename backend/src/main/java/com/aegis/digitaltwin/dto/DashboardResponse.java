package com.aegis.digitaltwin.dto;

public record DashboardResponse(
    long totalTwins,
    long totalEvents,
    long allowed,
    long stepUpAuth,
    long manualReview,
    long blocked,
    double fraudPressureIndex) {}
