package com.citizens.dti.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record FraudEvaluationRequest(
    UUID fraudEventId,
    UUID twinId,
    @NotBlank(message = "customerId is required") String customerId,
    String accountId,
    @NotBlank(message = "transactionId is required") String transactionId,
    @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        BigDecimal amount,
    String currency,
    String merchantName,
    String merchantCategory,
    String channel,
    String deviceId,
    String ipAddress,
    String geoCountry,
    String geoCity,
    Boolean knownDevice,
    Boolean knownBeneficiary,
    Integer failedLoginCountLast24h,
    Integer transactionCountLast1h,
    BigDecimal averageTransactionAmount30d,
    OffsetDateTime eventTime,
    Map<String, Object> digitalTwinContext) {}
