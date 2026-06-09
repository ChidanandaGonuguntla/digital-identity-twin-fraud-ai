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
@Table(name = "fraud_decision_audit", schema = "identity_twin")
public class FraudDecisionAuditEntity {

  @Id
  @Column(name = "assessment_id", nullable = false, length = 100)
  private String assessmentId;

  @Column(name = "transaction_id", nullable = false, length = 100)
  private String transactionId;

  @Column(name = "customer_id", nullable = false, length = 100)
  private String customerId;

  @Column(name = "decision", nullable = false, length = 30)
  private String decision;

  @Column(name = "final_score", nullable = false, precision = 10, scale = 2)
  private BigDecimal finalScore;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "score_breakdown_json", columnDefinition = "jsonb")
  private String scoreBreakdownJson;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "reason_codes_json", columnDefinition = "jsonb")
  private String reasonCodesJson;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "event_snapshot_json", columnDefinition = "jsonb")
  private String eventSnapshotJson;

  @Column(name = "amount", precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(name = "merchant_category", length = 120)
  private String merchantCategory;

  @Column(name = "device_id", length = 120)
  private String deviceId;

  @Column(name = "model_version", length = 100)
  private String modelVersion;

  @Column(name = "policy_version", length = 100)
  private String policyVersion;

  @Column(name = "feature_version", length = 100)
  private String featureVersion;

  @Column(name = "final_decision_reason", columnDefinition = "text")
  private String finalDecisionReason;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "feature_vector_json", columnDefinition = "jsonb")
  private String featureVectorJson;

  @Column(name = "challenged", nullable = false)
  private boolean challenged;

  @Column(name = "twin_updated", nullable = false)
  private boolean twinUpdated;

  @Column(name = "latency_ms")
  private long latencyMs;

  @Column(name = "assessed_at", nullable = false)
  private Instant assessedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "champion_score", precision = 10, scale = 4)
  private BigDecimal championScore;

  @Column(name = "challenger_score", precision = 10, scale = 4)
  private BigDecimal challengerScore;

  @Column(name = "score_delta", precision = 10, scale = 4)
  private BigDecimal scoreDelta;

  @Column(name = "model_agreement")
  private Boolean modelAgreement;

  @Column(name = "champion_model_version", length = 100)
  private String championModelVersion;

  @Column(name = "challenger_model_version", length = 100)
  private String challengerModelVersion;

  @PrePersist
  void prePersist() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }

  public String getAssessmentId() {
    return assessmentId;
  }

  public void setAssessmentId(String assessmentId) {
    this.assessmentId = assessmentId;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }

  public String getCustomerId() {
    return customerId;
  }

  public void setCustomerId(String customerId) {
    this.customerId = customerId;
  }

  public String getDecision() {
    return decision;
  }

  public void setDecision(String decision) {
    this.decision = decision;
  }

  public BigDecimal getFinalScore() {
    return finalScore;
  }

  public void setFinalScore(BigDecimal finalScore) {
    this.finalScore = finalScore;
  }

  public String getScoreBreakdownJson() {
    return scoreBreakdownJson;
  }

  public void setScoreBreakdownJson(String scoreBreakdownJson) {
    this.scoreBreakdownJson = scoreBreakdownJson;
  }

  public String getReasonCodesJson() {
    return reasonCodesJson;
  }

  public void setReasonCodesJson(String reasonCodesJson) {
    this.reasonCodesJson = reasonCodesJson;
  }

  public String getEventSnapshotJson() {
    return eventSnapshotJson;
  }

  public void setEventSnapshotJson(String eventSnapshotJson) {
    this.eventSnapshotJson = eventSnapshotJson;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getMerchantCategory() {
    return merchantCategory;
  }

  public void setMerchantCategory(String merchantCategory) {
    this.merchantCategory = merchantCategory;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  public String getModelVersion() {
    return modelVersion;
  }

  public void setModelVersion(String modelVersion) {
    this.modelVersion = modelVersion;
  }

  public String getPolicyVersion() {
    return policyVersion;
  }

  public void setPolicyVersion(String policyVersion) {
    this.policyVersion = policyVersion;
  }

  public String getFeatureVersion() {
    return featureVersion;
  }

  public void setFeatureVersion(String featureVersion) {
    this.featureVersion = featureVersion;
  }

  public String getFinalDecisionReason() {
    return finalDecisionReason;
  }

  public void setFinalDecisionReason(String finalDecisionReason) {
    this.finalDecisionReason = finalDecisionReason;
  }

  public String getFeatureVectorJson() {
    return featureVectorJson;
  }

  public void setFeatureVectorJson(String featureVectorJson) {
    this.featureVectorJson = featureVectorJson;
  }

  public boolean isChallenged() {
    return challenged;
  }

  public void setChallenged(boolean challenged) {
    this.challenged = challenged;
  }

  public boolean isTwinUpdated() {
    return twinUpdated;
  }

  public void setTwinUpdated(boolean twinUpdated) {
    this.twinUpdated = twinUpdated;
  }

  public long getLatencyMs() {
    return latencyMs;
  }

  public void setLatencyMs(long latencyMs) {
    this.latencyMs = latencyMs;
  }

  public Instant getAssessedAt() {
    return assessedAt;
  }

  public void setAssessedAt(Instant assessedAt) {
    this.assessedAt = assessedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public BigDecimal getChampionScore() {
    return championScore;
  }

  public void setChampionScore(BigDecimal championScore) {
    this.championScore = championScore;
  }

  public BigDecimal getChallengerScore() {
    return challengerScore;
  }

  public void setChallengerScore(BigDecimal challengerScore) {
    this.challengerScore = challengerScore;
  }

  public BigDecimal getScoreDelta() {
    return scoreDelta;
  }

  public void setScoreDelta(BigDecimal scoreDelta) {
    this.scoreDelta = scoreDelta;
  }

  public Boolean getModelAgreement() {
    return modelAgreement;
  }

  public void setModelAgreement(Boolean modelAgreement) {
    this.modelAgreement = modelAgreement;
  }

  public String getChampionModelVersion() {
    return championModelVersion;
  }

  public void setChampionModelVersion(String championModelVersion) {
    this.championModelVersion = championModelVersion;
  }

  public String getChallengerModelVersion() {
    return challengerModelVersion;
  }

  public void setChallengerModelVersion(String challengerModelVersion) {
    this.challengerModelVersion = challengerModelVersion;
  }
}
