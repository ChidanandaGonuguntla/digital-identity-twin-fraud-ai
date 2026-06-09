package com.citizens.digital.twin.api.dto;

public record MerchantCategoryMemoryItem(
    String category, String displayCategory, int count, double frequency, double riskOverlay) {}
