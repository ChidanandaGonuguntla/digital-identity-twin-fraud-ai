package com.citizens.digital.twin.api.dto;

import com.citizens.digital.twin.domain.model.RiskSignal;
import com.citizens.digital.twin.domain.model.ScoreBreakdown;
import com.citizens.digital.twin.domain.model.TransactionEvent;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record FraudDecisionAuditDetailResponse(
    String assessmentId,
    String transactionId,
    String customerId,
    String decision,
    double finalScore,
    ScoreBreakdown scoreBreakdown,
    List<RiskSignal> reasonCodes,
    TransactionEvent eventSnapshot,
    String modelVersion,
    String policyVersion,
    String featureVersion,
    String finalDecisionReason,
    Map<String, Double> featureVector,
    boolean challenged,
    boolean twinUpdated,
    long latencyMs,
    Instant assessedAt,
    Double championScore,
    Double challengerScore,
    Double scoreDelta,
    Boolean modelAgreement,
    String championModelVersion,
    String challengerModelVersion) {}
