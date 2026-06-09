package com.citizens.digital.twin.api.dto;

import java.util.Set;

public record TwinSummaryResponse(
    String customerId,
    long transactionCount,
    double averageAmount,
    Set<String> knownDevices,
    Set<String> usualCountries) {}
