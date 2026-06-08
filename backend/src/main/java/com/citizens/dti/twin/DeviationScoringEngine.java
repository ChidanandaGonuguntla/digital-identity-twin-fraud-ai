package com.citizens.dti.twin;

import com.citizens.dti.config.ScoringProperties;
import com.citizens.dti.model.BehavioralProfile;
import com.citizens.dti.model.Decision;
import com.citizens.dti.model.IdentityTwin;
import com.citizens.dti.model.RiskAssessment;
import com.citizens.dti.model.TransactionEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Reconciles a live event against the customer's twin and produces an explainable risk score.
 *
 * <p>Each fraud signal contributes weighted points; the cumulative score maps to an ALLOW /
 * CHALLENGE / BLOCK decision.
 */
@Component
public class DeviationScoringEngine {

  private static final double EARTH_RADIUS_KM = 6371.0;

  private static final long CATEGORY_VELOCITY_WINDOW_SECONDS = 30;

  private static final Set<String> HIGH_RISK_CATEGORIES =
      Set.of("jewellery", "jewelry", "electronics", "crypto", "gift_card", "gift card", "travel");

  private final ScoringProperties props;

  public DeviationScoringEngine(ScoringProperties props) {
    this.props = props;
  }

  public RiskAssessment score(IdentityTwin twin, TransactionEvent event) {
    BehavioralProfile profile = twin.getProfile();
    List<String> reasons = new ArrayList<>();

    if (profile.getTransactionCount() < props.minHistory()) {
      reasons.add(
          "Insufficient history (%d/%d txns) — twin still learning, observing only"
              .formatted(profile.getTransactionCount(), props.minHistory()));
      return new RiskAssessment(
          event.transactionId(),
          event.customerId(),
          0.0,
          Decision.ALLOW,
          reasons,
          true,
          Instant.now());
    }

    double score = 0.0;

    SignalResult amountSignal = scoreAmount(profile, event);
    SignalResult geoVelocitySignal = scoreGeoVelocity(profile, event);
    SignalResult newDeviceSignal = scoreNewDevice(profile, event);
    SignalResult unusualHourSignal = scoreUnusualHour(profile, event);
    SignalResult newCategorySignal = scoreNewCategory(profile, event);
    SignalResult categoryVelocitySignal = scoreCategoryVelocity(profile, event);
    SignalResult highRiskCategorySignal = scoreHighRiskCategory(event);

    score += addSignal(amountSignal, reasons);
    score += addSignal(geoVelocitySignal, reasons);
    score += addSignal(newDeviceSignal, reasons);
    score += addSignal(unusualHourSignal, reasons);
    score += addSignal(newCategorySignal, reasons);
    score += addSignal(categoryVelocitySignal, reasons);
    score += addSignal(highRiskCategorySignal, reasons);

    score = Math.min(100.0, score);

    if (reasons.isEmpty()) {
      reasons.add("Transaction consistent with the customer's behavioral twin");
    }

    Decision decision = decisionFromScore(score);

    decision =
        applyDecisionOverrides(
            decision,
            amountSignal.triggered(),
            categoryVelocitySignal.triggered(),
            highRiskCategorySignal.triggered(),
            newDeviceSignal.triggered(),
            geoVelocitySignal.triggered());

    return new RiskAssessment(
        event.transactionId(),
        event.customerId(),
        round(score),
        decision,
        reasons,
        false,
        Instant.now());
  }

  private double addSignal(SignalResult signal, List<String> reasons) {
    if (signal.triggered()) {
      reasons.add(signal.reason());
      return signal.score();
    }
    return 0.0;
  }

  private Decision decisionFromScore(double score) {
    if (score >= props.blockThreshold()) {
      return Decision.BLOCK;
    }

    if (score >= props.challengeThreshold()) {
      return Decision.CHALLENGE;
    }

    return Decision.ALLOW;
  }

  /**
   * Prevents logically inconsistent output like:
   *
   * <p>Reason codes are suspicious, but decision is ALLOW.
   */
  private Decision applyDecisionOverrides(
      Decision currentDecision,
      boolean highAmount,
      boolean categoryVelocity,
      boolean highRiskCategory,
      boolean newDevice,
      boolean impossibleTravel) {

    if (currentDecision != Decision.ALLOW) {
      return currentDecision;
    }

    /*
     * travel -> jewellery within seconds
     * + jewellery is high-risk
     * + amount is abnormal
     *
     * This should never remain ALLOW.
     */
    if (categoryVelocity && highRiskCategory && highAmount) {
      return Decision.CHALLENGE;
    }

    /*
     * High-risk category from a new/unknown device should require step-up.
     */
    if (highRiskCategory && newDevice) {
      return Decision.CHALLENGE;
    }

    /*
     * Impossible travel should at least challenge even if score threshold is low.
     */
    if (impossibleTravel) {
      return Decision.CHALLENGE;
    }

    return currentDecision;
  }

  private SignalResult scoreAmount(BehavioralProfile profile, TransactionEvent event) {
    double mean = profile.amountMean();
    double sd = Math.max(profile.amountStdDev(), mean * 0.10 + 1.0);
    double z = (event.amount() - mean) / sd;

    if (z > 3.0) {
      return SignalResult.hit(
          props.amountWeight(),
          "Amount $%.2f is %.1fσ above the customer's typical $%.2f"
              .formatted(event.amount(), z, mean));
    }

    if (z > 2.0) {
      return SignalResult.hit(
          props.amountWeight() * 0.6,
          "Amount $%.2f is elevated (%.1fσ above typical $%.2f)"
              .formatted(event.amount(), z, mean));
    }

    return SignalResult.none();
  }

  private SignalResult scoreGeoVelocity(BehavioralProfile profile, TransactionEvent event) {
    if (profile.getLastLatitude() == null || profile.getLastTimestampEpochSeconds() == null) {
      return SignalResult.none();
    }

    double distanceKm =
        haversine(
            profile.getLastLatitude(),
            profile.getLastLongitude(),
            event.latitude(),
            event.longitude());

    long elapsedSeconds =
        event.timestamp().getEpochSecond() - profile.getLastTimestampEpochSeconds();

    if (elapsedSeconds <= 0) {
      if (distanceKm > 50) {
        return SignalResult.hit(
            props.geoVelocityWeight(),
            "Simultaneous transactions %.0f km apart".formatted(distanceKm));
      }
      return SignalResult.none();
    }

    double speedKmh = distanceKm / (elapsedSeconds / 3600.0);

    if (speedKmh > props.impossibleSpeedKmh()) {
      return SignalResult.hit(
          props.geoVelocityWeight(),
          "Impossible travel: %.0f km in %d min implies %.0f km/h"
              .formatted(distanceKm, elapsedSeconds / 60, speedKmh));
    }

    if (speedKmh > props.impossibleSpeedKmh() * 0.4) {
      return SignalResult.hit(
          props.geoVelocityWeight() * 0.5,
          "Unusually fast travel between transactions (%.0f km/h)".formatted(speedKmh));
    }

    return SignalResult.none();
  }

  private SignalResult scoreNewDevice(BehavioralProfile profile, TransactionEvent event) {
    if (!profile.isKnownDevice(event.deviceId())) {
      return SignalResult.hit(
          props.newDeviceWeight(),
          "Unrecognized device fingerprint '%s'".formatted(event.deviceId()));
    }

    return SignalResult.none();
  }

  private SignalResult scoreUnusualHour(BehavioralProfile profile, TransactionEvent event) {
    double freq = profile.hourFrequency(event.hourOfDay());

    if (freq < 0.02) {
      return SignalResult.hit(
          props.unusualHourWeight(),
          "Transaction at %02d:00 UTC — customer rarely transacts at this hour"
              .formatted(event.hourOfDay()));
    }

    return SignalResult.none();
  }

  private SignalResult scoreNewCategory(BehavioralProfile profile, TransactionEvent event) {
    double freq = profile.categoryFrequency(event.merchantCategory());

    if (freq == 0.0) {
      return SignalResult.hit(
          props.newCategoryWeight(),
          "First-ever transaction in merchant category '%s'".formatted(event.merchantCategory()));
    }

    return SignalResult.none();
  }

  private SignalResult scoreCategoryVelocity(BehavioralProfile profile, TransactionEvent event) {
    if (profile.getLastTimestampEpochSeconds() == null
        || profile.getLastMerchantCategory() == null
        || event.merchantCategory() == null) {
      return SignalResult.none();
    }

    long elapsedSeconds =
        event.timestamp().getEpochSecond() - profile.getLastTimestampEpochSeconds();

    if (elapsedSeconds < 0 || elapsedSeconds > CATEGORY_VELOCITY_WINDOW_SECONDS) {
      return SignalResult.none();
    }

    String previousCategory = normalize(profile.getLastMerchantCategory());
    String currentCategory = normalize(event.merchantCategory());

    if (!previousCategory.equals(currentCategory)) {
      return SignalResult.hit(
          props.categoryVelocityWeight(),
          "Category velocity anomaly: %s → %s within %d seconds"
              .formatted(
                  profile.getLastMerchantCategory(), event.merchantCategory(), elapsedSeconds));
    }

    return SignalResult.none();
  }

  private SignalResult scoreHighRiskCategory(TransactionEvent event) {
    String category = normalize(event.merchantCategory());

    if (HIGH_RISK_CATEGORIES.contains(category)) {
      return SignalResult.hit(
          props.highRiskCategoryWeight(),
          "High-risk category: %s".formatted(event.merchantCategory()));
    }

    return SignalResult.none();
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase();
  }

  private double haversine(double lat1, double lon1, double lat2, double lon2) {
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);

    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);

    return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }

  private double round(double v) {
    return Math.round(v * 100.0) / 100.0;
  }

  private record SignalResult(boolean triggered, double score, String reason) {

    static SignalResult hit(double score, String reason) {
      return new SignalResult(true, score, reason);
    }

    static SignalResult none() {
      return new SignalResult(false, 0.0, null);
    }
  }
}
