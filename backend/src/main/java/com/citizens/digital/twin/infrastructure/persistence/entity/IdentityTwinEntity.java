package com.citizens.digital.twin.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "identity_twins", schema = "identity_twin")
public class IdentityTwinEntity {
  @Id private String customerId;
  private long transactionCount;
  private double amountSum;
  private double amountSumOfSquares;

  @Column(columnDefinition = "text")
  private String knownDevicesCsv;

  @Column(columnDefinition = "text")
  private String usualCountriesCsv;

  @Column(columnDefinition = "text")
  private String merchantCategoryCountsJson;

  @Column(columnDefinition = "text")
  private String hourHistogramCsv;

  private Double lastLatitude;
  private Double lastLongitude;
  private Long lastTimestampEpochSeconds;
  private String lastMerchantCategory;
  private Instant createdAt;
  private Instant updatedAt;
  @Version private Long version;

  public String getCustomerId() {
    return customerId;
  }

  public void setCustomerId(String v) {
    customerId = v;
  }

  public long getTransactionCount() {
    return transactionCount;
  }

  public void setTransactionCount(long v) {
    transactionCount = v;
  }

  public double getAmountSum() {
    return amountSum;
  }

  public void setAmountSum(double v) {
    amountSum = v;
  }

  public double getAmountSumOfSquares() {
    return amountSumOfSquares;
  }

  public void setAmountSumOfSquares(double v) {
    amountSumOfSquares = v;
  }

  public String getKnownDevicesCsv() {
    return knownDevicesCsv;
  }

  public void setKnownDevicesCsv(String v) {
    knownDevicesCsv = v;
  }

  public String getUsualCountriesCsv() {
    return usualCountriesCsv;
  }

  public void setUsualCountriesCsv(String v) {
    usualCountriesCsv = v;
  }

  public String getMerchantCategoryCountsJson() {
    return merchantCategoryCountsJson;
  }

  public void setMerchantCategoryCountsJson(String v) {
    merchantCategoryCountsJson = v;
  }

  public String getHourHistogramCsv() {
    return hourHistogramCsv;
  }

  public void setHourHistogramCsv(String v) {
    hourHistogramCsv = v;
  }

  public Double getLastLatitude() {
    return lastLatitude;
  }

  public void setLastLatitude(Double v) {
    lastLatitude = v;
  }

  public Double getLastLongitude() {
    return lastLongitude;
  }

  public void setLastLongitude(Double v) {
    lastLongitude = v;
  }

  public Long getLastTimestampEpochSeconds() {
    return lastTimestampEpochSeconds;
  }

  public void setLastTimestampEpochSeconds(Long v) {
    lastTimestampEpochSeconds = v;
  }

  public String getLastMerchantCategory() {
    return lastMerchantCategory;
  }

  public void setLastMerchantCategory(String v) {
    lastMerchantCategory = v;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant v) {
    createdAt = v;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant v) {
    updatedAt = v;
  }
}
