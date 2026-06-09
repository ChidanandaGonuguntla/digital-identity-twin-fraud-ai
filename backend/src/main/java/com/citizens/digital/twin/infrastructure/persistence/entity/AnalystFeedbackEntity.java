package com.citizens.digital.twin.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "analyst_feedback", schema = "identity_twin")
public class AnalystFeedbackEntity {

  @Id
  @Column(name = "feedback_id", nullable = false, length = 100)
  private String feedbackId;

  @Column(name = "assessment_id", nullable = false, length = 100, unique = true)
  private String assessmentId;

  @Column(name = "transaction_id", nullable = false, length = 100)
  private String transactionId;

  @Column(name = "customer_id", nullable = false, length = 100)
  private String customerId;

  @Column(name = "outcome", nullable = false, length = 40)
  private String outcome;

  @Column(name = "analyst_id", nullable = false, length = 255)
  private String analystId;

  @Column(name = "notes", columnDefinition = "text")
  private String notes;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @PrePersist
  void prePersist() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }

  public String getFeedbackId() {
    return feedbackId;
  }

  public void setFeedbackId(String feedbackId) {
    this.feedbackId = feedbackId;
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

  public String getOutcome() {
    return outcome;
  }

  public void setOutcome(String outcome) {
    this.outcome = outcome;
  }

  public String getAnalystId() {
    return analystId;
  }

  public void setAnalystId(String analystId) {
    this.analystId = analystId;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
