package com.citizens.digital.twin.api.dto;

import java.time.Instant;

public record FeatureStoreEntryResponse(
    String entityKey, String featureName, double featureValue, Instant updatedAt) {}
