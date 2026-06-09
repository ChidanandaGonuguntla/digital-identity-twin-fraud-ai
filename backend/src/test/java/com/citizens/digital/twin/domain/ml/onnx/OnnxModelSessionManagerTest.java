package com.citizens.digital.twin.domain.ml.onnx;

import static org.assertj.core.api.Assertions.assertThat;

import ai.onnxruntime.OrtException;
import com.citizens.digital.twin.infrastructure.config.MlModelProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class OnnxModelSessionManagerTest {

  private static final float[] FRAUD_LIKE = {
    0.8f, 0.9f, 0.7f, 0.9f, 0.8f, 1f, 0.85f, 0.9f, 0.1f, 0.9f
  };
  private static final float[] LEGIT_LIKE = {
    0.2f, 0.1f, 0.4f, 0.1f, 0.05f, 0f, 0.2f, 0.15f, 0.7f, 0.1f
  };

  @Test
  void loadActiveModel_runsFraudInference() throws OrtException {
    OnnxModelSessionManager manager = manager(false);

    manager.loadActiveModel();

    assertThat(manager.isReady()).isTrue();
    assertThat(manager.currentModel()).isPresent();
    assertThat(manager.currentModel().get().artifact().modelType()).isEqualTo("xgboost-onnx");

    double fraudProbability = manager.predict(FRAUD_LIKE);
    double legitProbability = manager.predict(LEGIT_LIKE);

    assertThat(fraudProbability).isGreaterThan(0.9);
    assertThat(legitProbability).isLessThan(0.5);
    assertThat(fraudProbability).isGreaterThan(legitProbability);
  }

  @Test
  void rollback_loadsPreviousVersion() {
    OnnxModelSessionManager manager = manager(false);

    manager.loadActiveModel();
    manager.rollback();

    assertThat(manager.isReady()).isTrue();
    assertThat(manager.currentModel()).isPresent();
    assertThat(manager.currentModel().get().version()).isEqualTo("fraud-risk-v0.9.0");
  }

  @Test
  void loadActiveModel_loadsChampionAndChallengerWhenEnabled() throws OrtException {
    OnnxModelSessionManager manager = manager(true);

    manager.loadActiveModel();

    assertThat(manager.isReady()).isTrue();
    assertThat(manager.isChallengerReady()).isTrue();
    assertThat(manager.currentModel()).isPresent();
    assertThat(manager.currentChallengerModel()).isPresent();
    assertThat(manager.currentModel().get().version()).isEqualTo("fraud-risk-v1.0.0");
    assertThat(manager.currentChallengerModel().get().version()).isEqualTo("fraud-risk-v0.9.0");

    double championScore = manager.predict(FRAUD_LIKE);
    double challengerScore = manager.predictChallenger(FRAUD_LIKE);

    assertThat(championScore).isGreaterThan(0.5);
    assertThat(challengerScore).isGreaterThan(0.5);
  }

  private OnnxModelSessionManager manager(boolean challengerEnabled) {
    MlModelProperties properties =
        new MlModelProperties(
            "onnx",
            "classpath:models",
            "fraud-risk-v1.0.0",
            "fraud-risk-v0.9.0",
            "fraud-risk-v1.0.0",
            "fraud-risk-v0.9.0",
            challengerEnabled,
            0.15,
            0.35);
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    return new OnnxModelSessionManager(properties, new DefaultResourceLoader(), objectMapper);
  }
}
