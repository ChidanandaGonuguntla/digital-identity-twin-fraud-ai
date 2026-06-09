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
@Table(name = "model_metrics", schema = "identity_twin")
public class ModelMetricsEntity {

  @Id
  @Column(name = "metric_id", nullable = false, length = 100)
  private String metricId;

  @Column(name = "assessment_id", length = 100)
  private String assessmentId;

  @Column(name = "model_name", nullable = false, length = 120)
  private String modelName;

  @Column(name = "model_version", nullable = false, length = 100)
  private String modelVersion;

  @Column(name = "precision_score", precision = 8, scale = 4)
  private BigDecimal precisionScore;

  @Column(name = "recall_score", precision = 8, scale = 4)
  private BigDecimal recallScore;

  @Column(name = "auc_score", precision = 8, scale = 4)
  private BigDecimal aucScore;

  @Column(name = "f1_score", precision = 8, scale = 4)
  private BigDecimal f1Score;

  @Column(name = "drift_score", precision = 8, scale = 4)
  private BigDecimal driftScore;

  @Column(name = "latency_ms", nullable = false)
  private long latencyMs;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "feature_snapshot", nullable = false, columnDefinition = "jsonb")
  private String featureSnapshot;

  @Column(name = "recorded_at", nullable = false)
  private Instant recordedAt;

  @PrePersist
  void prePersist() {
    if (recordedAt == null) recordedAt = Instant.now();
    if (featureSnapshot == null) featureSnapshot = "{}";
  }

  public String getMetricId() {
    return metricId;
  }

  public void setMetricId(String metricId) {
    this.metricId = metricId;
  }

  public String getAssessmentId() {
    return assessmentId;
  }

  public void setAssessmentId(String assessmentId) {
    this.assessmentId = assessmentId;
  }

  public String getModelName() {
    return modelName;
  }

  public void setModelName(String modelName) {
    this.modelName = modelName;
  }

  public String getModelVersion() {
    return modelVersion;
  }

  public void setModelVersion(String modelVersion) {
    this.modelVersion = modelVersion;
  }

  public BigDecimal getPrecisionScore() {
    return precisionScore;
  }

  public void setPrecisionScore(BigDecimal precisionScore) {
    this.precisionScore = precisionScore;
  }

  public BigDecimal getRecallScore() {
    return recallScore;
  }

  public void setRecallScore(BigDecimal recallScore) {
    this.recallScore = recallScore;
  }

  public BigDecimal getAucScore() {
    return aucScore;
  }

  public void setAucScore(BigDecimal aucScore) {
    this.aucScore = aucScore;
  }

  public BigDecimal getF1Score() {
    return f1Score;
  }

  public void setF1Score(BigDecimal f1Score) {
    this.f1Score = f1Score;
  }

  public BigDecimal getDriftScore() {
    return driftScore;
  }

  public void setDriftScore(BigDecimal driftScore) {
    this.driftScore = driftScore;
  }

  public long getLatencyMs() {
    return latencyMs;
  }

  public void setLatencyMs(long latencyMs) {
    this.latencyMs = latencyMs;
  }

  public String getFeatureSnapshot() {
    return featureSnapshot;
  }

  public void setFeatureSnapshot(String featureSnapshot) {
    this.featureSnapshot = featureSnapshot;
  }

  public Instant getRecordedAt() {
    return recordedAt;
  }

  public void setRecordedAt(Instant recordedAt) {
    this.recordedAt = recordedAt;
  }
}
