package com.citizens.dti.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * JPA mapping for {@code identity_twin.customer_twin}. The running aggregates are typed columns;
 * the document-shaped behavioral state is the {@code baseline} JSONB column.
 */
@Entity
@Table(name = "customer_twin", schema = "identity_twin")
public class CustomerTwinEntity {

  @Id
  @Column(name = "customer_id")
  private String customerId;

  @Column(name = "transaction_count", nullable = false)
  private long transactionCount;

  @Column(name = "amount_sum", nullable = false)
  private double amountSum;

  @Column(name = "amount_sum_sq", nullable = false)
  private double amountSumSq;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "baseline", columnDefinition = "jsonb", nullable = false)
  private TwinBaseline baseline = new TwinBaseline();

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "last_updated", nullable = false)
  private Instant lastUpdated;

  protected CustomerTwinEntity() {
    // for JPA
  }

  public String getCustomerId() {
    return customerId;
  }

  public void setCustomerId(String customerId) {
    this.customerId = customerId;
  }

  public long getTransactionCount() {
    return transactionCount;
  }

  public void setTransactionCount(long transactionCount) {
    this.transactionCount = transactionCount;
  }

  public double getAmountSum() {
    return amountSum;
  }

  public void setAmountSum(double amountSum) {
    this.amountSum = amountSum;
  }

  public double getAmountSumSq() {
    return amountSumSq;
  }

  public void setAmountSumSq(double amountSumSq) {
    this.amountSumSq = amountSumSq;
  }

  public TwinBaseline getBaseline() {
    return baseline;
  }

  public void setBaseline(TwinBaseline baseline) {
    this.baseline = baseline;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(Instant lastUpdated) {
    this.lastUpdated = lastUpdated;
  }
}
