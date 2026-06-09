package com.citizens.digital.twin.domain.ml;

import com.citizens.digital.twin.application.service.ModelRegistryService;
import com.citizens.digital.twin.domain.ml.onnx.OnnxFraudModelService;
import com.citizens.digital.twin.domain.model.IdentityTwin;
import com.citizens.digital.twin.domain.model.TransactionEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class RoutedMlFraudModelService implements MlFraudModelService {
  private final ModelRegistryService modelRegistryService;
  private final OnnxFraudModelService onnxFraudModelService;
  private final EmbeddedHeuristicMlFraudModelService heuristicMlFraudModelService;

  public RoutedMlFraudModelService(
      ModelRegistryService modelRegistryService,
      OnnxFraudModelService onnxFraudModelService,
      EmbeddedHeuristicMlFraudModelService heuristicMlFraudModelService) {
    this.modelRegistryService = modelRegistryService;
    this.onnxFraudModelService = onnxFraudModelService;
    this.heuristicMlFraudModelService = heuristicMlFraudModelService;
  }

  @Override
  @CircuitBreaker(name = "onnxMl", fallbackMethod = "heuristicFallback")
  public MlFraudPrediction predict(IdentityTwin twin, TransactionEvent event) {
    if (modelRegistryService.useOnnx()) {
      return onnxFraudModelService.predict(twin, event);
    }
    return heuristicMlFraudModelService.predict(twin, event);
  }

  private MlFraudPrediction heuristicFallback(
      IdentityTwin twin, TransactionEvent event, Throwable throwable) {
    return heuristicMlFraudModelService.predict(twin, event);
  }

  @Override
  public ModelMetadata metadata() {
    if (modelRegistryService.useOnnx()) {
      return onnxFraudModelService.metadata();
    }
    return heuristicMlFraudModelService.metadata();
  }
}
