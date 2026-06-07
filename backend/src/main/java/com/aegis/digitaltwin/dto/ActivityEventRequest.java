package com.aegis.digitaltwin.dto;

import com.aegis.digitaltwin.domain.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ActivityEventRequest(
        @NotBlank String customerId,
        @NotNull EventType eventType,
        BigDecimal amount,
        String merchant,
        String payee,
        @NotBlank String deviceId,
        @NotBlank String location,
        String ipAddress,
        Integer loginHour,
        String merchantCategory
) {}
