package com.citizens.dti.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fraud_step_up_challenge", schema = "identity_twin")
public class FraudStepUpChallengeEntity {

  @Id
  @Column(name = "challenge_id", nullable = false)
  private UUID challengeId;

  @Column(name = "fraud_event_id", nullable = false)
  private UUID fraudEventId;

  @Column(name = "customer_id", nullable = false)
  private String customerId;

  @Column(name = "account_id")
  private String accountId;

  @Column(name = "transaction_id")
  private String transactionId;

  @Column(name = "challenge_type", nullable = false)
  private String challengeType;

  @Column(name = "challenge_status", nullable = false)
  private String challengeStatus;

  @Column(name = "delivery_channel", nullable = false)
  private String deliveryChannel;

  @Column(name = "destination_label")
  private String destinationLabel;

  @Column(name = "reason_code")
  private String reasonCode;

  @Column(name = "reason_description")
  private String reasonDescription;

  @Column(name = "rule_score")
  private BigDecimal ruleScore;

  @Column(name = "ml_score")
  private BigDecimal mlScore;

  @Column(name = "final_risk_score")
  private BigDecimal finalRiskScore;

  @Column(name = "approval_token_hash")
  private String approvalTokenHash;

  @Column(name = "expires_at", nullable = false)
  private OffsetDateTime expiresAt;

  @Column(name = "approved_at")
  private OffsetDateTime approvedAt;

  @Column(name = "denied_at")
  private OffsetDateTime deniedAt;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  public UUID getChallengeId() {
    return challengeId;
  }

  public void setChallengeId(UUID challengeId) {
    this.challengeId = challengeId;
  }

  public UUID getFraudEventId() {
    return fraudEventId;
  }

  public void setFraudEventId(UUID fraudEventId) {
    this.fraudEventId = fraudEventId;
  }

  public String getCustomerId() {
    return customerId;
  }

  public void setCustomerId(String customerId) {
    this.customerId = customerId;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
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

  public String getDestinationLabel() {
    return destinationLabel;
  }

  public void setDestinationLabel(String destinationLabel) {
    this.destinationLabel = destinationLabel;
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

  public String getApprovalTokenHash() {
    return approvalTokenHash;
  }

  public void setApprovalTokenHash(String approvalTokenHash) {
    this.approvalTokenHash = approvalTokenHash;
  }

  public OffsetDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(OffsetDateTime expiresAt) {
    this.expiresAt = expiresAt;
  }

  public OffsetDateTime getApprovedAt() {
    return approvedAt;
  }

  public void setApprovedAt(OffsetDateTime approvedAt) {
    this.approvedAt = approvedAt;
  }

  public OffsetDateTime getDeniedAt() {
    return deniedAt;
  }

  public void setDeniedAt(OffsetDateTime deniedAt) {
    this.deniedAt = deniedAt;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
