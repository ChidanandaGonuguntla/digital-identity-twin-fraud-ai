package com.citizens.dti.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The learned behavioral baseline of a customer — the substance of the digital twin.
 *
 * <p>Holds running aggregates so the twin can be updated incrementally with O(1) work per event.
 */
public class BehavioralProfile {

  private long transactionCount = 0;

  // Amount running statistics
  private double amountSum = 0.0;
  private double amountSumOfSquares = 0.0;

  private final Set<String> knownDevices = new HashSet<>();
  private final Map<String, Integer> merchantCategoryCounts = new HashMap<>();
  private final long[] hourHistogram = new long[24];

  private Double lastLatitude;
  private Double lastLongitude;
  private Long lastTimestampEpochSeconds;

  // Required for category velocity anomaly:
  // Example: travel -> jewellery within 30 seconds.
  private String lastMerchantCategory;

  public long getTransactionCount() {
    return transactionCount;
  }

  public double amountMean() {
    return transactionCount == 0 ? 0.0 : amountSum / transactionCount;
  }

  public double amountStdDev() {
    if (transactionCount < 2) {
      return 0.0;
    }

    double mean = amountMean();
    double variance = (amountSumOfSquares / transactionCount) - (mean * mean);
    return variance <= 0 ? 0.0 : Math.sqrt(variance);
  }

  public boolean isKnownDevice(String deviceId) {
    if (deviceId == null || deviceId.isBlank()) {
      return false;
    }

    return knownDevices.contains(normalize(deviceId));
  }

  /** Fraction of historical transactions in the given merchant category. */
  public double categoryFrequency(String category) {
    if (transactionCount == 0 || category == null || category.isBlank()) {
      return 0.0;
    }

    return merchantCategoryCounts.getOrDefault(normalize(category), 0) / (double) transactionCount;
  }

  /** Fraction of historical transactions in the given hour of day. */
  public double hourFrequency(int hourOfDay) {
    if (transactionCount == 0 || hourOfDay < 0 || hourOfDay > 23) {
      return 0.0;
    }

    return hourHistogram[hourOfDay] / (double) transactionCount;
  }

  public Double getLastLatitude() {
    return lastLatitude;
  }

  public Double getLastLongitude() {
    return lastLongitude;
  }

  public Long getLastTimestampEpochSeconds() {
    return lastTimestampEpochSeconds;
  }

  public String getLastMerchantCategory() {
    return lastMerchantCategory;
  }

  /**
   * Synchronize the baseline with one observed event.
   *
   * <p>Important: this method should be called only after scoring is completed. If you call apply()
   * before scoring, lastMerchantCategory will already equal the current category and category
   * velocity detection will not work.
   */
  public void apply(TransactionEvent event) {
    transactionCount++;

    amountSum += event.amount();
    amountSumOfSquares += event.amount() * event.amount();

    if (event.deviceId() != null && !event.deviceId().isBlank()) {
      knownDevices.add(normalize(event.deviceId()));
    }

    if (event.merchantCategory() != null && !event.merchantCategory().isBlank()) {
      merchantCategoryCounts.merge(normalize(event.merchantCategory()), 1, Integer::sum);
      lastMerchantCategory = normalize(event.merchantCategory());
    }

    if (event.hourOfDay() >= 0 && event.hourOfDay() <= 23) {
      hourHistogram[event.hourOfDay()]++;
    }

    lastLatitude = event.latitude();
    lastLongitude = event.longitude();
    lastTimestampEpochSeconds = event.timestamp().getEpochSecond();
  }

  // ---- Raw-state accessors used by persistence mapper ----

  public double getAmountSum() {
    return amountSum;
  }

  public double getAmountSumOfSquares() {
    return amountSumOfSquares;
  }

  public Set<String> getKnownDevices() {
    return Set.copyOf(knownDevices);
  }

  public Map<String, Integer> getMerchantCategoryCounts() {
    return Map.copyOf(merchantCategoryCounts);
  }

  public long[] getHourHistogram() {
    return hourHistogram.clone();
  }

  /** Rebuild a profile from persisted state. */
  public static BehavioralProfile restore(
      long transactionCount,
      double amountSum,
      double amountSumOfSquares,
      Set<String> knownDevices,
      Map<String, Integer> merchantCategoryCounts,
      long[] hourHistogram,
      Double lastLatitude,
      Double lastLongitude,
      Long lastTimestampEpochSeconds,
      String lastMerchantCategory) {

    BehavioralProfile p = new BehavioralProfile();

    p.transactionCount = transactionCount;
    p.amountSum = amountSum;
    p.amountSumOfSquares = amountSumOfSquares;

    if (knownDevices != null) {
      knownDevices.stream()
          .filter(v -> v != null && !v.isBlank())
          .map(BehavioralProfile::normalize)
          .forEach(p.knownDevices::add);
    }

    if (merchantCategoryCounts != null) {
      merchantCategoryCounts.forEach(
          (category, count) -> {
            if (category != null && !category.isBlank() && count != null) {
              p.merchantCategoryCounts.put(normalize(category), count);
            }
          });
    }

    if (hourHistogram != null) {
      System.arraycopy(hourHistogram, 0, p.hourHistogram, 0, Math.min(24, hourHistogram.length));
    }

    p.lastLatitude = lastLatitude;
    p.lastLongitude = lastLongitude;
    p.lastTimestampEpochSeconds = lastTimestampEpochSeconds;
    p.lastMerchantCategory =
        lastMerchantCategory == null || lastMerchantCategory.isBlank()
            ? null
            : normalize(lastMerchantCategory);

    return p;
  }

  private static String normalize(String value) {
    return value.trim().toLowerCase();
  }
}
