package com.citizens.dti.twin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.citizens.dti.config.ScoringProperties;
import com.citizens.dti.model.Decision;
import com.citizens.dti.model.IdentityTwin;
import com.citizens.dti.model.RiskAssessment;
import com.citizens.dti.model.TransactionEvent;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeviationScoringEngineTest {

  private DeviationScoringEngine engine;
  private IdentityTwin twin;

  // Charlotte, NC
  private static final double HOME_LAT = 35.2271;
  private static final double HOME_LON = -80.8431;
  private static final Instant BASE = Instant.parse("2026-06-01T14:00:00Z");

  @BeforeEach
  void setUp() {
    ScoringProperties props = new ScoringProperties(5, 30, 40, 20, 15, 15, 40, 70, 900);
    engine = new DeviationScoringEngine(props);
    twin = new IdentityTwin("CUST-001");

    // Build a stable baseline: ~$50 grocery spend, same device, midday, Charlotte.
    for (int i = 0; i < 10; i++) {
      twin.getProfile()
          .apply(
              new TransactionEvent(
                  "CUST-001",
                  "seed-" + i,
                  50.0 + i,
                  "GROCERY",
                  "device-trusted",
                  HOME_LAT,
                  HOME_LON,
                  BASE.plus(i, ChronoUnit.DAYS)));
    }
  }

  @Test
  void normalTransactionIsAllowed() {
    TransactionEvent normal =
        new TransactionEvent(
            "CUST-001",
            "txn-normal",
            55.0,
            "GROCERY",
            "device-trusted",
            HOME_LAT,
            HOME_LON,
            BASE.plus(11, ChronoUnit.DAYS));

    RiskAssessment result = engine.score(twin, normal);

    assertEquals(Decision.ALLOW, result.decision());
    assertTrue(result.riskScore() < 40, "expected low risk, got " + result.riskScore());
  }

  @Test
  void impossibleTravelWithNewDeviceIsBlocked() {
    // Last seed was in Charlotte at BASE+9d. This hits Singapore minutes later.
    TransactionEvent fraud =
        new TransactionEvent(
            "CUST-001",
            "txn-fraud",
            1200.0,
            "ELECTRONICS",
            "device-unknown",
            1.3521,
            103.8198,
            BASE.plus(9, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES));

    RiskAssessment result = engine.score(twin, fraud);

    assertEquals(Decision.BLOCK, result.decision());
    assertTrue(result.riskScore() >= 70, "expected high risk, got " + result.riskScore());
    assertTrue(result.reasons().stream().anyMatch(r -> r.contains("Impossible travel")));
  }

  @Test
  void newCustomerColdStartIsObservedNotPunished() {
    IdentityTwin fresh = new IdentityTwin("CUST-NEW");
    TransactionEvent first =
        new TransactionEvent(
            "CUST-NEW", "txn-first", 9999.0, "GAMBLING", "device-x", 0.0, 0.0, BASE);

    RiskAssessment result = engine.score(fresh, first);

    assertEquals(Decision.ALLOW, result.decision());
    assertTrue(result.coldStart());
  }
}
