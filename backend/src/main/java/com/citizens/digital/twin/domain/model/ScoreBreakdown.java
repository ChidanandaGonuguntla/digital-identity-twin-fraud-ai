package com.citizens.digital.twin.domain.model;

import java.math.BigDecimal;

public record ScoreBreakdown(
    BigDecimal ruleScore,
    BigDecimal twinDeviationScore,
    BigDecimal mlScore,
    BigDecimal graphScore,
    BigDecimal finalScore) {}
