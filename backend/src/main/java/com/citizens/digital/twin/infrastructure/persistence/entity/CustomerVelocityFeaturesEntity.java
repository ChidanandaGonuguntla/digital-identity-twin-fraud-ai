package com.citizens.digital.twin.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "customer_velocity_features", schema = "identity_twin")
public class CustomerVelocityFeaturesEntity {

  @Id
  @Column(name = "customer_id", nullable = false, length = 100)
  private String customerId;

  @Column(name = "txn_count_5m", nullable = false)
  private int txnCount5m;

  @Column(name = "txn_count_1h", nullable = false)
  private int txnCount1h;

  @Column(name = "txn_count_24h", nullable = false)
  private int txnCount24h;

  @Column(name = "amount_sum_1h", nullable = false, precision = 14, scale = 2)
  private BigDecimal amountSum1h;

  @Column(name = "new_devices_24h", nullable = false)
  private int newDevices24h;

  @Column(name = "countries_24h", nullable = false)
  private int countries24h;

  @Column(name = "category_changes_10m", nullable = false)
  private int categoryChanges10m;

  @Column(name = "failed_attempts_30m", nullable = false)
  private int failedAttempts30m;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void prePersist() {
    if (updatedAt == null) {
      updatedAt = Instant.now();
    }
    if (amountSum1h == null) {
      amountSum1h = BigDecimal.ZERO;
    }
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = Instant.now();
  }

  public String getCustomerId() {
    return customerId;
  }

  public void setCustomerId(String customerId) {
    this.customerId = customerId;
  }

  public int getTxnCount5m() {
    return txnCount5m;
  }

  public void setTxnCount5m(int txnCount5m) {
    this.txnCount5m = txnCount5m;
  }

  public int getTxnCount1h() {
    return txnCount1h;
  }

  public void setTxnCount1h(int txnCount1h) {
    this.txnCount1h = txnCount1h;
  }

  public int getTxnCount24h() {
    return txnCount24h;
  }

  public void setTxnCount24h(int txnCount24h) {
    this.txnCount24h = txnCount24h;
  }

  public BigDecimal getAmountSum1h() {
    return amountSum1h;
  }

  public void setAmountSum1h(BigDecimal amountSum1h) {
    this.amountSum1h = amountSum1h;
  }

  public int getNewDevices24h() {
    return newDevices24h;
  }

  public void setNewDevices24h(int newDevices24h) {
    this.newDevices24h = newDevices24h;
  }

  public int getCountries24h() {
    return countries24h;
  }

  public void setCountries24h(int countries24h) {
    this.countries24h = countries24h;
  }

  public int getCategoryChanges10m() {
    return categoryChanges10m;
  }

  public void setCategoryChanges10m(int categoryChanges10m) {
    this.categoryChanges10m = categoryChanges10m;
  }

  public int getFailedAttempts30m() {
    return failedAttempts30m;
  }

  public void setFailedAttempts30m(int failedAttempts30m) {
    this.failedAttempts30m = failedAttempts30m;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
