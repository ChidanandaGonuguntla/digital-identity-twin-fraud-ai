package com.citizens.digital.twin.api.dto;

import com.citizens.digital.twin.domain.model.RiskSignal;
import com.citizens.digital.twin.domain.model.ScoreBreakdown;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ModelExplainabilityReportResponse(
    String assessmentId,
    String transactionId,
    String customerId,
    String decision,
    double finalScore,
    String modelVersion,
    String featureSchemaVersion,
    String policyVersion,
    String finalDecisionReason,
    ScoreBreakdown scoreBreakdown,
    List<RiskSignal> reasonCodes,
    Map<String, Double> featureVector,
    List<FeatureContributionResponse> topFeatures,
    Instant assessedAt) {}
