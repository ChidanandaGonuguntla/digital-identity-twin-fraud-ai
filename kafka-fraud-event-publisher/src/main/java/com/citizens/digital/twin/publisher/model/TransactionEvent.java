package com.citizens.digital.twin.publisher.model;

import java.time.Instant;
import java.util.Map;

public record TransactionEvent(
    String customerId,
    String transactionId,
    double amount,
    String currency,
    String merchantCategory,
    String merchantName,
    String merchantId,
    String deviceId,
    String ipAddress,
    String userAgent,
    String channel,
    String paymentInstrumentId,
    double latitude,
    double longitude,
    String countryCode,
    String city,
    Instant timestamp,
    Map<String, Object> metadata) {}
