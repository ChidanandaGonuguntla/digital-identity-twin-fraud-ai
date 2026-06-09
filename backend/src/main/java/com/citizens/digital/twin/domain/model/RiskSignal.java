package com.citizens.digital.twin.domain.model;

import java.math.BigDecimal;

public record RiskSignal(
    RiskSignalType type,
    String code,
    String message,
    Severity severity,
    BigDecimal scoreContribution,
    String evidence) {}
