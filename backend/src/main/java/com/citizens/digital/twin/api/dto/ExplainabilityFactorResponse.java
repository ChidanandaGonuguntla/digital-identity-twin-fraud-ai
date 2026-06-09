package com.citizens.digital.twin.api.dto;

public record ExplainabilityFactorResponse(
    String label, String detail, double points, String source) {}
