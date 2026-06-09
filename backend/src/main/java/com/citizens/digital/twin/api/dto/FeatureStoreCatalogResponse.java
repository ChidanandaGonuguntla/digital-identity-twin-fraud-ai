package com.citizens.digital.twin.api.dto;

import java.util.List;

public record FeatureStoreCatalogResponse(List<String> featureNames, long totalEntries) {}
