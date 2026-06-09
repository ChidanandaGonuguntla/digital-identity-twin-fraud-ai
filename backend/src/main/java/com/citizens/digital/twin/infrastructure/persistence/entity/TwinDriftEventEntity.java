package com.citizens.digital.twin.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "twin_drift_events", schema = "identity_twin")
public class TwinDriftEventEntity {

  @Id
  @Column(name = "drift_event_id", nullable = false, length = 100)
  private String driftEventId;

  @Column(name = "assessment_id", nullable = false, length = 100)
  private String assessmentId;

  @Column(name = "customer_id", nullable = false, length = 100)
  private String customerId;

  @Column(name = "transaction_id", nullable = false, length = 100)
  private String transactionId;

  @Column(name = "drift_score", nullable = false, precision = 10, scale = 2)
  private BigDecimal driftScore;

  @Column(name = "drift_threshold", nullable = false, precision = 10, scale = 2)
  private BigDecimal driftThreshold;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "signal_snapshot", nullable = false, columnDefinition = "jsonb")
  private String signalSnapshot;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "baseline_snapshot", nullable = false, columnDefinition = "jsonb")
  private String baselineSnapshot;

  @Column(name = "detected_at", nullable = false)
  private Instant detectedAt;

  @PrePersist
  void prePersist() {
    if (detectedAt == null) detectedAt = Instant.now();
    if (signalSnapshot == null) signalSnapshot = "[]";
    if (baselineSnapshot == null) baselineSnapshot = "{}";
  }

  public String getDriftEventId() {
    return driftEventId;
  }

  public void setDriftEventId(String driftEventId) {
    this.driftEventId = driftEventId;
  }

  public String getAssessmentId() {
    return assessmentId;
  }

  public void setAssessmentId(String assessmentId) {
    this.assessmentId = assessmentId;
  }

  public String getCustomerId() {
    return customerId;
  }

  public void setCustomerId(String customerId) {
    this.customerId = customerId;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }

  public BigDecimal getDriftScore() {
    return driftScore;
  }

  public void setDriftScore(BigDecimal driftScore) {
    this.driftScore = driftScore;
  }

  public BigDecimal getDriftThreshold() {
    return driftThreshold;
  }

  public void setDriftThreshold(BigDecimal driftThreshold) {
    this.driftThreshold = driftThreshold;
  }

  public String getSignalSnapshot() {
    return signalSnapshot;
  }

  public void setSignalSnapshot(String signalSnapshot) {
    this.signalSnapshot = signalSnapshot;
  }

  public String getBaselineSnapshot() {
    return baselineSnapshot;
  }

  public void setBaselineSnapshot(String baselineSnapshot) {
    this.baselineSnapshot = baselineSnapshot;
  }

  public Instant getDetectedAt() {
    return detectedAt;
  }

  public void setDetectedAt(Instant detectedAt) {
    this.detectedAt = detectedAt;
  }
}
