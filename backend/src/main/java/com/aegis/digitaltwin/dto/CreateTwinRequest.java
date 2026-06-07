package com.aegis.digitaltwin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateTwinRequest(
        @NotBlank String customerId,
        String fullName,
        String email,
        String phone,
        String homeCity,
        String homeCountry,
        String segment,
        List<String> knownDevices,
        List<String> knownLocations,
        List<String> trustedMerchants,
        List<String> trustedPayees,
        @NotNull BigDecimal averageTransactionAmount
) {}
