package com.citizens.digital.twin.domain.ml.feature;

import static org.assertj.core.api.Assertions.assertThat;

import com.citizens.digital.twin.domain.ml.FraudFeatureVector;
import com.citizens.digital.twin.domain.model.IdentityTwin;
import com.citizens.digital.twin.domain.model.TransactionEvent;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FraudFeatureEngineeringServiceTest {

  private final FraudFeatureEngineeringService service = new FraudFeatureEngineeringService();

  @Test
  void build_producesStableOrderedVector() {
    IdentityTwin twin = IdentityTwin.newTwin("CUST-001");
    TransactionEvent event =
        new TransactionEvent(
            "CUST-001",
            "TXN-001",
            250.0,
            "USD",
            "GROCERY",
            null,
            null,
            "dev-1",
            null,
            null,
            "MOBILE",
            null,
            35.0,
            -80.0,
            "US",
            null,
            Instant.parse("2026-06-08T14:00:00Z"),
            null);

    FraudFeatureVector vector = service.build(twin, event);

    assertThat(vector.featureOrder()).isEqualTo(FraudFeatureVector.FEATURE_ORDER);
    assertThat(vector.values()).hasSize(FraudFeatureVector.FEATURE_ORDER.size());
    assertThat(vector.namedValues())
        .containsKeys(FraudFeatureVector.FEATURE_ORDER.toArray(String[]::new));
  }
}
