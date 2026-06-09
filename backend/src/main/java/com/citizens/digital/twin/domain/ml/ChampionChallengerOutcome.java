package com.citizens.digital.twin.domain.ml;

import java.math.BigDecimal;

public record ChampionChallengerOutcome(
    MlFraudPrediction champion,
    MlFraudPrediction challenger,
    BigDecimal scoreDelta,
    boolean modelAgreement) {}
