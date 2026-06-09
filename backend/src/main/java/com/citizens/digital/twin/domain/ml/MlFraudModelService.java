package com.citizens.digital.twin.domain.ml;

import com.citizens.digital.twin.domain.model.IdentityTwin;
import com.citizens.digital.twin.domain.model.TransactionEvent;

public interface MlFraudModelService {
  MlFraudPrediction predict(IdentityTwin twin, TransactionEvent event);

  ModelMetadata metadata();
}
