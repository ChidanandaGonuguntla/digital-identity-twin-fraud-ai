package com.citizens.digital.twin.api.dto;

import java.time.Instant;

public record CustomerVelocityResponse(
    String customerId,
    int txnCount5m,
    int txnCount1h,
    int txnCount24h,
    double amountSum1h,
    int newDevices24h,
    int countries24h,
    int categoryChanges10m,
    int failedAttempts30m,
    Instant updatedAt) {}
