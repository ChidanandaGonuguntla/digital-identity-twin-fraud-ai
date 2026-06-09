package com.citizens.digital.twin.domain.ml.feature;

import com.citizens.digital.twin.domain.ml.FraudFeatureVector;
import com.citizens.digital.twin.domain.model.BehavioralProfile;
import com.citizens.digital.twin.domain.model.IdentityTwin;
import com.citizens.digital.twin.domain.model.TransactionEvent;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class FraudFeatureEngineeringService {
  private static final double EARTH_RADIUS_KM = 6371.0;
  private static final Set<String> HIGH_RISK_CHANNELS = Set.of("TOR", "PROXY", "UNKNOWN");
  private static final Set<String> HIGH_RISK_CATEGORIES =
      Set.of("electronics", "jewelry", "jewellery", "crypto", "gift_card", "gift card", "travel");

  public FraudFeatureVector build(IdentityTwin twin, TransactionEvent event) {
    BehavioralProfile profile = twin == null ? new BehavioralProfile() : twin.getProfile();
    LinkedHashMap<String, Double> features = new LinkedHashMap<>();
    double amount = Math.max(event.amount(), 0.01);
    double mean = profile.amountMean();
    double std = profile.amountStdDev();
    features.put("amount_log", Math.log1p(amount) / Math.log1p(10_000.0));
    features.put("amount_zscore", std <= 0.0 ? 0.0 : clamp((amount - mean) / std, -4.0, 4.0) / 4.0);
    features.put("hour_of_day_norm", event.hourOfDay() / 23.0);
    features.put("is_new_device", isNewDevice(profile, event) ? 1.0 : 0.0);
    features.put("is_new_country", isNewCountry(twin, event) ? 1.0 : 0.0);
    features.put("is_high_risk_channel", isHighRiskChannel(event) ? 1.0 : 0.0);
    features.put("merchant_risk_score", merchantRiskScore(event.merchantCategory()));
    features.put("category_frequency", 1.0 - profile.categoryFrequency(event.merchantCategory()));
    features.put("twin_txn_count_norm", clamp(profile.getTransactionCount(), 0.0, 200.0) / 200.0);
    features.put("geo_distance_norm", geoDistanceNorm(profile, event));
    return FraudFeatureVector.of(features);
  }

  public Map<String, Double> syntheticSample(boolean fraud) {
    LinkedHashMap<String, Double> sample = new LinkedHashMap<>();
    sample.put("amount_log", fraud ? 0.82 : 0.35);
    sample.put("amount_zscore", fraud ? 0.75 : 0.05);
    sample.put("hour_of_day_norm", fraud ? 0.92 : 0.45);
    sample.put("is_new_device", fraud ? 1.0 : 0.0);
    sample.put("is_new_country", fraud ? 1.0 : 0.0);
    sample.put("is_high_risk_channel", fraud ? 0.0 : 0.0);
    sample.put("merchant_risk_score", fraud ? 0.9 : 0.2);
    sample.put("category_frequency", fraud ? 0.95 : 0.15);
    sample.put("twin_txn_count_norm", fraud ? 0.05 : 0.55);
    sample.put("geo_distance_norm", fraud ? 0.88 : 0.05);
    return sample;
  }

  private boolean isNewDevice(BehavioralProfile profile, TransactionEvent event) {
    return event.deviceId() != null
        && !event.deviceId().isBlank()
        && profile.getTransactionCount() > 0
        && !profile.isKnownDevice(event.deviceId());
  }

  private boolean isNewCountry(IdentityTwin twin, TransactionEvent event) {
    return twin != null
        && !twin.usualCountries().isEmpty()
        && event.countryCode() != null
        && !event.countryCode().isBlank()
        && !twin.isUsualCountry(event.countryCode());
  }

  private boolean isHighRiskChannel(TransactionEvent event) {
    return event.channel() != null
        && HIGH_RISK_CHANNELS.contains(event.channel().trim().toUpperCase(Locale.ROOT));
  }

  private double merchantRiskScore(String category) {
    if (category == null || category.isBlank()) {
      return 0.2;
    }
    return HIGH_RISK_CATEGORIES.contains(category.trim().toLowerCase(Locale.ROOT)) ? 0.9 : 0.2;
  }

  private double geoDistanceNorm(BehavioralProfile profile, TransactionEvent event) {
    if (!event.hasGeoLocation()
        || profile.getLastLatitude() == null
        || profile.getLastLongitude() == null) {
      return 0.0;
    }
    double distanceKm =
        haversineKm(
            profile.getLastLatitude(),
            profile.getLastLongitude(),
            event.latitude(),
            event.longitude());
    return clamp(distanceKm, 0.0, 12_000.0) / 12_000.0;
  }

  private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
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

  private double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
}
