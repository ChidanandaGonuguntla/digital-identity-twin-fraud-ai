package com.citizens.digital.twin.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "step_up_challenges", schema = "identity_twin")
public class StepUpChallengeEntity {

  @Id
  @Column(name = "challenge_id", nullable = false, length = 100)
  private String challengeId;

  @Column(name = "assessment_id", nullable = false, length = 100)
  private String assessmentId;

  @Column(name = "customer_id", nullable = false, length = 100)
  private String customerId;

  @Column(name = "transaction_id", nullable = false, length = 100)
  private String transactionId;

  @Column(name = "challenge_type", nullable = false, length = 60)
  private String challengeType;

  @Column(name = "challenge_status", nullable = false, length = 40)
  private String challengeStatus;

  @Column(name = "delivery_channel", nullable = false, length = 60)
  private String deliveryChannel;

  @Column(name = "reason_code", length = 100)
  private String reasonCode;

  @Column(name = "reason_description", length = 500)
  private String reasonDescription;

  @Column(name = "rule_score", precision = 10, scale = 2)
  private BigDecimal ruleScore;

  @Column(name = "ml_score", precision = 10, scale = 2)
  private BigDecimal mlScore;

  @Column(name = "final_risk_score", nullable = false, precision = 10, scale = 2)
  private BigDecimal finalRiskScore;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "explainability_json", nullable = false, columnDefinition = "jsonb")
  private String explainabilityJson;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "rejected_at")
  private Instant rejectedAt;

  @Column(name = "expired_at")
  private Instant expiredAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void prePersist() {
    Instant now = Instant.now();
    if (createdAt == null) createdAt = now;
    if (updatedAt == null) updatedAt = now;
    if (explainabilityJson == null) explainabilityJson = "{}";
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = Instant.now();
  }

  public String getChallengeId() {
    return challengeId;
  }

  public void setChallengeId(String challengeId) {
    this.challengeId = challengeId;
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

  public String getChallengeType() {
    return challengeType;
  }

  public void setChallengeType(String challengeType) {
    this.challengeType = challengeType;
  }

  public String getChallengeStatus() {
    return challengeStatus;
  }

  public void setChallengeStatus(String challengeStatus) {
    this.challengeStatus = challengeStatus;
  }

  public String getDeliveryChannel() {
    return deliveryChannel;
  }

  public void setDeliveryChannel(String deliveryChannel) {
    this.deliveryChannel = deliveryChannel;
  }

  public String getReasonCode() {
    return reasonCode;
  }

  public void setReasonCode(String reasonCode) {
    this.reasonCode = reasonCode;
  }

  public String getReasonDescription() {
    return reasonDescription;
  }

  public void setReasonDescription(String reasonDescription) {
    this.reasonDescription = reasonDescription;
  }

  public BigDecimal getRuleScore() {
    return ruleScore;
  }

  public void setRuleScore(BigDecimal ruleScore) {
    this.ruleScore = ruleScore;
  }

  public BigDecimal getMlScore() {
    return mlScore;
  }

  public void setMlScore(BigDecimal mlScore) {
    this.mlScore = mlScore;
  }

  public BigDecimal getFinalRiskScore() {
    return finalRiskScore;
  }

  public void setFinalRiskScore(BigDecimal finalRiskScore) {
    this.finalRiskScore = finalRiskScore;
  }

  public String getExplainabilityJson() {
    return explainabilityJson;
  }

  public void setExplainabilityJson(String explainabilityJson) {
    this.explainabilityJson = explainabilityJson;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Instant getApprovedAt() {
    return approvedAt;
  }

  public void setApprovedAt(Instant approvedAt) {
    this.approvedAt = approvedAt;
  }

  public Instant getRejectedAt() {
    return rejectedAt;
  }

  public void setRejectedAt(Instant rejectedAt) {
    this.rejectedAt = rejectedAt;
  }

  public Instant getExpiredAt() {
    return expiredAt;
  }

  public void setExpiredAt(Instant expiredAt) {
    this.expiredAt = expiredAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
