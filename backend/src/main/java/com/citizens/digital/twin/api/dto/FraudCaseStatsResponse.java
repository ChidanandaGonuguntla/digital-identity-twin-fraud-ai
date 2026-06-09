package com.citizens.digital.twin.api.dto;

public record FraudCaseStatsResponse(
    long totalCases,
    long activeCases,
    long openCases,
    long assignedCases,
    long inReviewCases,
    long waitingCustomerCases,
    long confirmedFraudCases,
    long falsePositiveCases,
    long closedCases,
    long highPriorityCases,
    long slaBreachedCases) {}
