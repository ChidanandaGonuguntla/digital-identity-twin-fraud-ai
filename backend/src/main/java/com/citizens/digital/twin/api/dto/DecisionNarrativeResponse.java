package com.citizens.digital.twin.api.dto;

import java.util.List;

public record DecisionNarrativeResponse(
    String assessmentId,
    String decision,
    double finalScore,
    String headline,
    String narrative,
    List<String> bullets,
    List<ExplainabilityFactorResponse> factors,
    ScoreAttributionResponse scoreAttribution,
    List<ShapFeatureResponse> shapFeatures,
    List<FeatureContributionResponse> topFeatures,
    double mlProbability,
    Double championScore,
    Double challengerScore,
    Double scoreDelta,
    Boolean modelAgreement) {}
