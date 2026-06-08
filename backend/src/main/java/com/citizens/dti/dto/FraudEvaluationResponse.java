package com.citizens.dti.dto;

import com.citizens.dti.enums.FraudDecision;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record FraudEvaluationResponse(
    UUID fraudEventId,
    UUID mlScoreId,
    UUID twinId,
    String customerId,
    String accountId,
    String transactionId,
    BigDecimal ruleScore,
    BigDecimal mlFraudProbability,
    BigDecimal mlScore,
    BigDecimal finalRiskScore,
    FraudDecision finalDecision,
    String modelName,
    String modelVersion,
    List<RiskSignalDto> topRiskSignals,
    StepUpChallengeResponse stepUpChallenge,
    Long inferenceLatencyMs,
    OffsetDateTime evaluatedAt) {}
