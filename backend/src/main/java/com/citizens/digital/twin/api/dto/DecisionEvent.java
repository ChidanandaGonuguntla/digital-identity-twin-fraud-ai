package com.citizens.digital.twin.api.dto;

import java.util.List;

public record DecisionEvent(
    String assessmentId,
    String transactionId,
    String customerId,
    double amount,
    String merchantCategory,
    String deviceId,
    double latitude,
    double longitude,
    long eventTimeEpochMs,
    double riskScore,
    String decision,
    boolean coldStart,
    List<SignalContribution> signals,
    List<String> reasons) {}
