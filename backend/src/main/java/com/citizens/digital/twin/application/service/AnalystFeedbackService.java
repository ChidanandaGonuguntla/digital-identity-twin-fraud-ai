package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.api.dto.AnalystFeedbackRequest;
import com.citizens.digital.twin.api.dto.AnalystFeedbackResponse;
import com.citizens.digital.twin.infrastructure.persistence.entity.AnalystFeedbackEntity;
import com.citizens.digital.twin.infrastructure.persistence.entity.FraudDecisionAuditEntity;
import com.citizens.digital.twin.infrastructure.persistence.repository.AnalystFeedbackJpaRepository;
import com.citizens.digital.twin.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalystFeedbackService {
  private static final Set<String> VALID_OUTCOMES =
      Set.of(
          "CONFIRMED_FRAUD",
          "FALSE_POSITIVE",
          "CUSTOMER_VERIFIED",
          "CUSTOMER_DENIED",
          "NEEDS_INVESTIGATION");

  private final AnalystFeedbackJpaRepository repository;
  private final FraudDecisionAuditService auditService;
  private final AnalystActionAuditService analystActionAuditService;

  public AnalystFeedbackService(
      AnalystFeedbackJpaRepository repository,
      FraudDecisionAuditService auditService,
      AnalystActionAuditService analystActionAuditService) {
    this.repository = repository;
    this.auditService = auditService;
    this.analystActionAuditService = analystActionAuditService;
  }

  @Transactional
  public AnalystFeedbackResponse submit(String assessmentId, AnalystFeedbackRequest request) {
    String outcome = normalizeOutcome(request.outcome());
    FraudDecisionAuditEntity audit =
        auditService
            .findByAssessmentId(assessmentId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Assessment not found: " + assessmentId));
    AnalystFeedbackEntity entity =
        repository.findByAssessmentId(assessmentId).orElseGet(AnalystFeedbackEntity::new);
    if (entity.getFeedbackId() == null) {
      entity.setFeedbackId("FB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
    entity.setAssessmentId(assessmentId);
    entity.setTransactionId(audit.getTransactionId());
    entity.setCustomerId(audit.getCustomerId());
    entity.setOutcome(outcome);
    entity.setAnalystId(currentActor());
    entity.setNotes(request.notes());
    repository.save(entity);
    analystActionAuditService.record(
        "ANALYST_FEEDBACK", "ASSESSMENT", assessmentId, java.util.Map.of("outcome", outcome));
    return toResponse(entity);
  }

  public List<AnalystFeedbackResponse> listRecent() {
    return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
  }

  public String exportCsv() {
    StringBuilder csv =
        new StringBuilder(
            "feedbackId,assessmentId,transactionId,customerId,outcome,analystId,notes,createdAt\n");
    for (AnalystFeedbackResponse row : listRecent()) {
      csv.append(csvEscape(row.feedbackId())).append(',');
      csv.append(csvEscape(row.assessmentId())).append(',');
      csv.append(csvEscape(row.transactionId())).append(',');
      csv.append(csvEscape(row.customerId())).append(',');
      csv.append(csvEscape(row.outcome())).append(',');
      csv.append(csvEscape(row.analystId())).append(',');
      csv.append(csvEscape(row.notes())).append(',');
      csv.append(row.createdAt() == null ? "" : row.createdAt()).append('\n');
    }
    return csv.toString();
  }

  private String normalizeOutcome(String outcome) {
    if (outcome == null || outcome.isBlank()) {
      throw new IllegalArgumentException("Feedback outcome is required");
    }
    String normalized = outcome.trim().toUpperCase();
    if (!VALID_OUTCOMES.contains(normalized)) {
      throw new IllegalArgumentException("Invalid feedback outcome: " + outcome);
    }
    return normalized;
  }

  private AnalystFeedbackResponse toResponse(AnalystFeedbackEntity entity) {
    return new AnalystFeedbackResponse(
        entity.getFeedbackId(),
        entity.getAssessmentId(),
        entity.getTransactionId(),
        entity.getCustomerId(),
        entity.getOutcome(),
        entity.getAnalystId(),
        entity.getNotes(),
        entity.getCreatedAt());
  }

  private String currentActor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication == null || authentication.getPrincipal() == null
        ? "system"
        : String.valueOf(authentication.getPrincipal());
  }

  private String csvEscape(String value) {
    if (value == null) {
      return "";
    }
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
