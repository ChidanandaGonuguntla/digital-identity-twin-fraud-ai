package com.citizens.dti.dto;

import java.math.BigDecimal;

public record RiskSignalDto(
    String code, String label, String severity, BigDecimal contribution, String description) {}
