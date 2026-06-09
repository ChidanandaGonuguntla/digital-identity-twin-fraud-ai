package com.citizens.digital.twin.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "fraud_cases", schema = "identity_twin")
public class FraudCaseEntity {

  @Id
  @Column(name = "case_id", nullable = false, length = 100)
  private String caseId;

  @Column(name = "assessment_id", length = 100)
  private String assessmentId;

  @Column(name = "transaction_id", nullable = false, length = 100)
  private String transactionId;

  @Column(name = "customer_id", nullable = false, length = 100)
  private String customerId;

  @Column(name = "status", nullable = false, length = 40)
  private String status;

  @Column(name = "priority", nullable = false, length = 20)
  private String priority;

  @Column(name = "assigned_to", length = 255)
  private String assignedTo;

  @Column(name = "sla_due_at")
  private Instant slaDueAt;

  @Column(name = "escalation_level", nullable = false)
  private int escalationLevel;

  @Column(name = "closure_reason", length = 80)
  private String closureReason;

  @Column(name = "summary", columnDefinition = "text")
  private String summary;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "closed_at")
  private Instant closedAt;

  @PrePersist
  void prePersist() {
    Instant now = Instant.now();
    if (createdAt == null) {
      createdAt = now;
    }
    if (updatedAt == null) {
      updatedAt = now;
    }
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = Instant.now();
  }

  public String getCaseId() {
    return caseId;
  }

  public void setCaseId(String caseId) {
    this.caseId = caseId;
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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  public String getAssignedTo() {
    return assignedTo;
  }

  public void setAssignedTo(String assignedTo) {
    this.assignedTo = assignedTo;
  }

  public Instant getSlaDueAt() {
    return slaDueAt;
  }

  public void setSlaDueAt(Instant slaDueAt) {
    this.slaDueAt = slaDueAt;
  }

  public int getEscalationLevel() {
    return escalationLevel;
  }

  public void setEscalationLevel(int escalationLevel) {
    this.escalationLevel = escalationLevel;
  }

  public String getClosureReason() {
    return closureReason;
  }

  public void setClosureReason(String closureReason) {
    this.closureReason = closureReason;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
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

  public Instant getClosedAt() {
    return closedAt;
  }

  public void setClosedAt(Instant closedAt) {
    this.closedAt = closedAt;
  }
}
