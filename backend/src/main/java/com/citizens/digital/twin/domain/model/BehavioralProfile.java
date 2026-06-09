package com.citizens.digital.twin.domain.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BehavioralProfile {
  private long transactionCount = 0;
  private double amountSum = 0.0;
  private double amountSumOfSquares = 0.0;
  private final Set<String> knownDevices = new HashSet<>();
  private final Set<String> usualCountries = new HashSet<>();
  private final Map<String, Integer> merchantCategoryCounts = new HashMap<>();
  private final long[] hourHistogram = new long[24];
  private Double lastLatitude;
  private Double lastLongitude;
  private Long lastTimestampEpochSeconds;
  private String lastMerchantCategory;

  public long getTransactionCount() {
    return transactionCount;
  }

  public double amountMean() {
    return transactionCount == 0 ? 0.0 : amountSum / transactionCount;
  }

  public double amountStdDev() {
    if (transactionCount < 2) return 0.0;
    double mean = amountMean();
    double variance = (amountSumOfSquares / transactionCount) - (mean * mean);
    return variance <= 0 ? 0.0 : Math.sqrt(variance);
  }

  public boolean isKnownDevice(String deviceId) {
    return deviceId != null && !deviceId.isBlank() && knownDevices.contains(normalize(deviceId));
  }

  public boolean isUsualCountry(String countryCode) {
    return countryCode != null
        && !countryCode.isBlank()
        && usualCountries.contains(normalizeCountry(countryCode));
  }

  public double categoryFrequency(String category) {
    if (transactionCount == 0 || category == null || category.isBlank()) return 0.0;
    return merchantCategoryCounts.getOrDefault(normalize(category), 0) / (double) transactionCount;
  }

  public double hourFrequency(int hourOfDay) {
    if (transactionCount == 0 || hourOfDay < 0 || hourOfDay > 23) return 0.0;
    return hourHistogram[hourOfDay] / (double) transactionCount;
  }

  public void apply(TransactionEvent event) {
    transactionCount++;
    amountSum += event.amount();
    amountSumOfSquares += event.amount() * event.amount();
    if (event.deviceId() != null && !event.deviceId().isBlank())
      knownDevices.add(normalize(event.deviceId()));
    if (event.countryCode() != null && !event.countryCode().isBlank())
      usualCountries.add(normalizeCountry(event.countryCode()));
    if (event.merchantCategory() != null && !event.merchantCategory().isBlank()) {
      merchantCategoryCounts.merge(normalize(event.merchantCategory()), 1, Integer::sum);
      lastMerchantCategory = normalize(event.merchantCategory());
    }
    if (event.hourOfDay() >= 0 && event.hourOfDay() <= 23) hourHistogram[event.hourOfDay()]++;
    lastLatitude = event.latitude();
    lastLongitude = event.longitude();
    lastTimestampEpochSeconds = event.timestamp().getEpochSecond();
  }

  public double getAmountSum() {
    return amountSum;
  }

  public double getAmountSumOfSquares() {
    return amountSumOfSquares;
  }

  public Set<String> getKnownDevices() {
    return Set.copyOf(knownDevices);
  }

  public Set<String> getUsualCountries() {
    return Set.copyOf(usualCountries);
  }

  public Map<String, Integer> getMerchantCategoryCounts() {
    return Map.copyOf(merchantCategoryCounts);
  }

  public long[] getHourHistogram() {
    return hourHistogram.clone();
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

  public static BehavioralProfile restore(
      long transactionCount,
      double amountSum,
      double amountSumOfSquares,
      Set<String> knownDevices,
      Set<String> usualCountries,
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
    if (knownDevices != null)
      knownDevices.stream()
          .filter(v -> v != null && !v.isBlank())
          .map(BehavioralProfile::normalize)
          .forEach(p.knownDevices::add);
    if (usualCountries != null)
      usualCountries.stream()
          .filter(v -> v != null && !v.isBlank())
          .map(BehavioralProfile::normalizeCountry)
          .forEach(p.usualCountries::add);
    if (merchantCategoryCounts != null)
      merchantCategoryCounts.forEach(
          (k, v) -> {
            if (k != null && v != null) p.merchantCategoryCounts.put(normalize(k), v);
          });
    if (hourHistogram != null)
      System.arraycopy(hourHistogram, 0, p.hourHistogram, 0, Math.min(24, hourHistogram.length));
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

  private static String normalizeCountry(String value) {
    return value.trim().toUpperCase();
  }
}
