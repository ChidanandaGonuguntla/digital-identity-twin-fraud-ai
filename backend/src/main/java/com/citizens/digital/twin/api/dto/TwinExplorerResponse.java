package com.citizens.digital.twin.api.dto;

import java.time.Instant;
import java.util.List;

public record TwinExplorerResponse(
    String customerId,
    long transactionCount,
    double amountMean,
    double amountStdDev,
    double amountTotal,
    String lastMerchantCategory,
    String topMerchantCategory,
    List<String> knownDevices,
    List<String> usualCountries,
    List<MerchantCategoryMemoryItem> merchantCategories,
    Instant createdAt,
    Instant updatedAt,
    boolean coldStart) {}
