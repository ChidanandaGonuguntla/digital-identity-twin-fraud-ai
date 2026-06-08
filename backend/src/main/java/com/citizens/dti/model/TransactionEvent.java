package com.citizens.dti.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

/**
 * A live transaction/authorization event arriving from the channel layer
 * (in production: a Kafka topic fed by card-auth or online-banking streams).
 * This is the "reported state" that gets reconciled against the customer's twin.
 */
public record TransactionEvent(
        @NotBlank String customerId,
        @NotBlank String transactionId,
        @Positive double amount,
        @NotBlank String merchantCategory,
        @NotBlank String deviceId,
        double latitude,
        double longitude,
        @NotNull Instant timestamp
) {
    public int hourOfDay() {
        return timestamp.atZone(java.time.ZoneOffset.UTC).getHour();
    }
}
