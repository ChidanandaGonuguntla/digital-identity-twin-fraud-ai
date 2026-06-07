package com.aegis.digitaltwin.dto;

import com.aegis.digitaltwin.domain.DecisionType;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record FraudDecisionResponse(
        String eventId,
        String customerId,
        int riskScore,
        DecisionType decision,
        List<String> reasons,
        Map<String, Integer> signalScores,
        Instant evaluatedAt
) {}
