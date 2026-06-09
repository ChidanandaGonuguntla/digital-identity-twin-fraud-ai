package com.citizens.digital.twin.domain.scoring;

import com.citizens.digital.twin.domain.model.*;
import com.citizens.digital.twin.infrastructure.config.ScoringProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class TwinDeviationScoringService {
  private static final double EARTH_RADIUS_KM = 6371.0;
  private static final long CATEGORY_VELOCITY_WINDOW_SECONDS = 30;
  private static final Set<String> HIGH_RISK_CATEGORIES =
      Set.of("jewellery", "jewelry", "electronics", "crypto", "gift_card", "gift card", "travel");
  private final ScoringProperties props;

  public TwinDeviationScoringService(ScoringProperties props) {
    this.props = props;
  }

  public TwinDeviationScore score(IdentityTwin twin, TransactionEvent event) {
    BehavioralProfile profile = twin.getProfile();
    List<RiskSignal> signals = new ArrayList<>();
    if (profile.getTransactionCount() < props.minHistory()) {
      signals.add(
          new RiskSignal(
              RiskSignalType.TWIN_DEVIATION,
              "TWIN_COLD_START",
              "Insufficient customer history; twin is still learning.",
              Severity.LOW,
              BigDecimal.ZERO,
              "transactionCount=" + profile.getTransactionCount()));
      return new TwinDeviationScore(BigDecimal.ZERO, signals, true);
    }
    add(signals, scoreAmount(profile, event));
    add(signals, scoreGeoVelocity(profile, event));
    add(signals, scoreNewDevice(profile, event));
    add(signals, scoreNewCountry(profile, event));
    add(signals, scoreUnusualHour(profile, event));
    add(signals, scoreNewCategory(profile, event));
    add(signals, scoreCategoryVelocity(profile, event));
    add(signals, scoreHighRiskCategory(event));
    BigDecimal score =
        signals.stream()
            .map(RiskSignal::scoreContribution)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .min(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);
    return new TwinDeviationScore(score, signals, false);
  }

  private void add(List<RiskSignal> out, RiskSignal signal) {
    if (signal != null) out.add(signal);
  }

  private RiskSignal hit(
      String code, String message, Severity severity, double score, String evidence) {
    return new RiskSignal(
        RiskSignalType.TWIN_DEVIATION,
        code,
        message,
        severity,
        BigDecimal.valueOf(score),
        evidence);
  }

  private RiskSignal scoreAmount(BehavioralProfile p, TransactionEvent e) {
    double mean = p.amountMean();
    double sd = Math.max(p.amountStdDev(), mean * 0.10 + 1.0);
    double z = (e.amount() - mean) / sd;
    if (z > 3.0)
      return hit(
          "AMOUNT_ANOMALY_CRITICAL",
          "Amount is materially above customer baseline.",
          Severity.HIGH,
          props.amountWeight(),
          "amount=" + e.amount() + ", mean=" + round(mean) + ", z=" + round(z));
    if (z > 2.0)
      return hit(
          "AMOUNT_ANOMALY_ELEVATED",
          "Amount is elevated above customer baseline.",
          Severity.MEDIUM,
          props.amountWeight() * 0.6,
          "amount=" + e.amount() + ", mean=" + round(mean) + ", z=" + round(z));
    return null;
  }

  private RiskSignal scoreGeoVelocity(BehavioralProfile p, TransactionEvent e) {
    if (p.getLastLatitude() == null
        || p.getLastLongitude() == null
        || p.getLastTimestampEpochSeconds() == null
        || !e.hasGeoLocation()) return null;
    double d = haversine(p.getLastLatitude(), p.getLastLongitude(), e.latitude(), e.longitude());
    long elapsed = e.timestamp().getEpochSecond() - p.getLastTimestampEpochSeconds();
    if (elapsed <= 0 && d > 50)
      return hit(
          "SIMULTANEOUS_GEO_DISTANCE",
          "Simultaneous transactions are geographically distant.",
          Severity.HIGH,
          props.geoVelocityWeight(),
          "distanceKm=" + round(d));
    if (elapsed <= 0) return null;
    double speed = d / (elapsed / 3600.0);
    if (speed > props.impossibleSpeedKmh())
      return hit(
          "IMPOSSIBLE_TRAVEL",
          "Impossible travel detected between sequential transactions.",
          Severity.CRITICAL,
          props.geoVelocityWeight(),
          "distanceKm=" + round(d) + ", elapsedSeconds=" + elapsed + ", speedKmh=" + round(speed));
    if (speed > props.impossibleSpeedKmh() * 0.4)
      return hit(
          "FAST_TRAVEL",
          "Unusually fast travel detected.",
          Severity.MEDIUM,
          props.geoVelocityWeight() * 0.5,
          "speedKmh=" + round(speed));
    return null;
  }

  private RiskSignal scoreNewDevice(BehavioralProfile p, TransactionEvent e) {
    return !p.isKnownDevice(e.deviceId())
        ? hit(
            "NEW_DEVICE",
            "Unrecognized device fingerprint.",
            Severity.MEDIUM,
            props.newDeviceWeight(),
            "deviceId=" + e.deviceId())
        : null;
  }

  private RiskSignal scoreNewCountry(BehavioralProfile p, TransactionEvent e) {
    if (e.countryCode() == null || e.countryCode().isBlank() || p.getUsualCountries().isEmpty())
      return null;
    return !p.isUsualCountry(e.countryCode())
        ? hit(
            "NEW_COUNTRY",
            "Transaction country is not part of customer's usual countries.",
            Severity.HIGH,
            props.newCountryWeight(),
            "countryCode=" + e.countryCode() + ", usualCountries=" + p.getUsualCountries())
        : null;
  }

  private RiskSignal scoreUnusualHour(BehavioralProfile p, TransactionEvent e) {
    double f = p.hourFrequency(e.hourOfDay());
    return f < 0.02
        ? hit(
            "UNUSUAL_HOUR",
            "Customer rarely transacts at this hour.",
            Severity.LOW,
            props.unusualHourWeight(),
            "hourOfDay=" + e.hourOfDay() + ", frequency=" + round(f))
        : null;
  }

  private RiskSignal scoreNewCategory(BehavioralProfile p, TransactionEvent e) {
    return p.categoryFrequency(e.merchantCategory()) == 0.0
        ? hit(
            "NEW_MERCHANT_CATEGORY",
            "First observed transaction in this merchant category.",
            Severity.MEDIUM,
            props.newCategoryWeight(),
            "merchantCategory=" + e.merchantCategory())
        : null;
  }

  private RiskSignal scoreCategoryVelocity(BehavioralProfile p, TransactionEvent e) {
    if (p.getLastTimestampEpochSeconds() == null
        || p.getLastMerchantCategory() == null
        || e.merchantCategory() == null) return null;
    long elapsed = e.timestamp().getEpochSecond() - p.getLastTimestampEpochSeconds();
    if (elapsed < 0 || elapsed > CATEGORY_VELOCITY_WINDOW_SECONDS) return null;
    String prev = normalize(p.getLastMerchantCategory());
    String cur = normalize(e.merchantCategory());
    return !prev.equals(cur)
        ? hit(
            "CATEGORY_VELOCITY",
            "Merchant category changed quickly between sequential events.",
            Severity.HIGH,
            props.categoryVelocityWeight(),
            "previous=" + prev + ", current=" + cur + ", elapsedSeconds=" + elapsed)
        : null;
  }

  private RiskSignal scoreHighRiskCategory(TransactionEvent e) {
    String c = normalize(e.merchantCategory());
    return HIGH_RISK_CATEGORIES.contains(c)
        ? hit(
            "HIGH_RISK_CATEGORY",
            "Merchant category is high risk.",
            Severity.MEDIUM,
            props.highRiskCategoryWeight(),
            "merchantCategory=" + e.merchantCategory())
        : null;
  }

  private String normalize(String v) {
    return v == null ? "" : v.trim().toLowerCase();
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

  public record TwinDeviationScore(BigDecimal score, List<RiskSignal> signals, boolean coldStart) {}
}
