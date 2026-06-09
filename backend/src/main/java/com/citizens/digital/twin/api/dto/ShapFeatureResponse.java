package com.citizens.digital.twin.api.dto;

public record ShapFeatureResponse(
    String feature, String displayName, double value, double shapValue, String impact) {}
