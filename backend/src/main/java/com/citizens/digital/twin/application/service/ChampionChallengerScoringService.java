package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.domain.ml.ChampionChallengerOutcome;
import com.citizens.digital.twin.domain.ml.EmbeddedHeuristicMlFraudModelService;
import com.citizens.digital.twin.domain.ml.MlFraudPrediction;
import com.citizens.digital.twin.domain.ml.onnx.OnnxFraudModelService;
import com.citizens.digital.twin.domain.model.IdentityTwin;
import com.citizens.digital.twin.domain.model.TransactionEvent;
import com.citizens.digital.twin.infrastructure.config.MlModelProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class ChampionChallengerScoringService {
  private final ModelRegistryService modelRegistryService;
  private final OnnxFraudModelService onnxFraudModelService;
  private final EmbeddedHeuristicMlFraudModelService heuristicMlFraudModelService;
  private final MlModelProperties properties;

  public ChampionChallengerScoringService(
      ModelRegistryService modelRegistryService,
      OnnxFraudModelService onnxFraudModelService,
      EmbeddedHeuristicMlFraudModelService heuristicMlFraudModelService,
      MlModelProperties properties) {
    this.modelRegistryService = modelRegistryService;
    this.onnxFraudModelService = onnxFraudModelService;
    this.heuristicMlFraudModelService = heuristicMlFraudModelService;
    this.properties = properties;
  }

  public ChampionChallengerOutcome score(IdentityTwin twin, TransactionEvent event) {
    MlFraudPrediction champion = predictChampion(twin, event);
    MlFraudPrediction challenger = null;
    if (properties.challengerEnabled() && modelRegistryService.useOnnx()) {
      challenger = onnxFraudModelService.predictChallenger(twin, event);
    }
    if (challenger == null) {
      return new ChampionChallengerOutcome(champion, null, BigDecimal.ZERO, true);
    }
    BigDecimal delta =
        champion.score().subtract(challenger.score()).abs().setScale(2, RoundingMode.HALF_UP);
    boolean agreement = delta.compareTo(BigDecimal.valueOf(10)) <= 0;
    return new ChampionChallengerOutcome(champion, challenger, delta, agreement);
  }

  private MlFraudPrediction predictChampion(IdentityTwin twin, TransactionEvent event) {
    if (modelRegistryService.useOnnx()) {
      return onnxFraudModelService.predict(twin, event);
    }
    return heuristicMlFraudModelService.predict(twin, event);
  }
}
