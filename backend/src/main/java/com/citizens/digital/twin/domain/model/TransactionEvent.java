package com.citizens.digital.twin.domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

public record TransactionEvent(
    @NotBlank String customerId,
    @NotBlank String transactionId,
    @Positive double amount,
    @NotBlank String currency,
    @NotBlank String merchantCategory,
    String merchantName,
    String merchantId,
    @NotBlank String deviceId,
    String ipAddress,
    String userAgent,
    @NotBlank String channel,
    String paymentInstrumentId,
    double latitude,
    double longitude,
    String countryCode,
    String city,
    @NotNull Instant timestamp,
    Map<String, Object> metadata) {
  public int hourOfDay() {
    return timestamp.atZone(ZoneOffset.UTC).getHour();
  }

  public boolean hasGeoLocation() {
    return latitude != 0.0 || longitude != 0.0;
  }

  public String eventKey() {
    return customerId + ":" + transactionId;
  }
}
