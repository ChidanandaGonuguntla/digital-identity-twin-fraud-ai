package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.api.dto.*;
import com.citizens.digital.twin.domain.model.Decision;
import com.citizens.digital.twin.domain.model.RiskAssessment;
import com.citizens.digital.twin.domain.model.TransactionEvent;
import com.citizens.digital.twin.infrastructure.persistence.entity.FraudCaseEntity;
import com.citizens.digital.twin.infrastructure.persistence.entity.FraudCaseEventEntity;
import com.citizens.digital.twin.infrastructure.persistence.entity.FraudDecisionAuditEntity;
import com.citizens.digital.twin.infrastructure.persistence.repository.FraudCaseEventJpaRepository;
import com.citizens.digital.twin.infrastructure.persistence.repository.FraudCaseJpaRepository;
import com.citizens.digital.twin.infrastructure.persistence.repository.FraudCaseSpecifications;
import com.citizens.digital.twin.infrastructure.persistence.repository.FraudDecisionAuditJpaRepository;
import com.citizens.digital.twin.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudCaseManagementService {
  private static final Set<String> VALID_STATUSES =
      Set.of(
          "OPEN",
          "ASSIGNED",
          "IN_REVIEW",
          "WAITING_CUSTOMER",
          "CONFIRMED_FRAUD",
          "FALSE_POSITIVE",
          "CLOSED");

  private final FraudCaseJpaRepository caseRepository;
  private final FraudCaseEventJpaRepository eventRepository;
  private final FraudDecisionAuditJpaRepository auditRepository;
  private final AnalystActionAuditService analystActionAuditService;
  private final ObjectMapper objectMapper;

  public FraudCaseManagementService(
      FraudCaseJpaRepository caseRepository,
      FraudCaseEventJpaRepository eventRepository,
      FraudDecisionAuditJpaRepository auditRepository,
      AnalystActionAuditService analystActionAuditService,
      ObjectMapper objectMapper) {
    this.caseRepository = caseRepository;
    this.eventRepository = eventRepository;
    this.auditRepository = auditRepository;
    this.analystActionAuditService = analystActionAuditService;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public void autoCreateFromAssessment(TransactionEvent event, RiskAssessment assessment) {
    if (assessment.decision() != Decision.BLOCK && assessment.decision() != Decision.CHALLENGE) {
      return;
    }
    if (caseRepository.findByAssessmentId(assessment.assessmentId()).isPresent()) {
      return;
    }
    FraudCaseEntity entity = new FraudCaseEntity();
    entity.setCaseId("CASE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    entity.setAssessmentId(assessment.assessmentId());
    entity.setTransactionId(assessment.transactionId());
    entity.setCustomerId(assessment.customerId());
    entity.setStatus("OPEN");
    entity.setPriority(priorityFor(assessment));
    entity.setEscalationLevel(0);
    entity.setSlaDueAt(Instant.now().plus(4, ChronoUnit.HOURS));
    entity.setSummary(
        assessment.decision().name()
            + " decision on "
            + event.merchantCategory()
            + " for $"
            + String.format("%.2f", event.amount()));
    caseRepository.save(entity);
    appendEvent(
        entity.getCaseId(), "CASE_CREATED", Map.of("decision", assessment.decision().name()));
  }

  public FraudCaseStatsResponse summary() {
    Object[] row = unwrapAggregateRow(caseRepository.summaryAggregate());
    long total = toLong(row[0]);
    long active = toLong(row[1]);
    long open = toLong(row[2]);
    long assigned = toLong(row[3]);
    long inReview = toLong(row[4]);
    long waitingCustomer = toLong(row[5]);
    long confirmedFraud = toLong(row[6]);
    long falsePositive = toLong(row[7]);
    long closed = toLong(row[8]);
    long highPriority = toLong(row[9]);
    long slaBreached = toLong(row[10]);
    return new FraudCaseStatsResponse(
        total,
        active,
        open,
        assigned,
        inReview,
        waitingCustomer,
        confirmedFraud,
        falsePositive,
        closed,
        highPriority,
        slaBreached);
  }

  @Transactional
  public FraudCaseBackfillResponse backfill(int limit) {
    int batchSize = Math.min(1000, Math.max(1, limit));
    Page<FraudDecisionAuditEntity> batch =
        auditRepository.findNeedingCase(PageRequest.of(0, batchSize));
    int created = 0;
    int skipped = 0;
    for (FraudDecisionAuditEntity audit : batch.getContent()) {
      if (caseRepository.findByAssessmentId(audit.getAssessmentId()).isPresent()) {
        skipped++;
        continue;
      }
      createCaseFromAudit(audit);
      created++;
    }
    long remaining = auditRepository.countNeedingCase();
    return new FraudCaseBackfillResponse(created, skipped, remaining);
  }

  public FraudCasePageResponse list(
      int page,
      int size,
      String status,
      String customerId,
      String transactionId,
      String assignedTo,
      String priority) {
    int safePage = Math.max(0, page);
    int safeSize = Math.min(100, Math.max(1, size));
    PageRequest pageable =
        PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "updatedAt"));
    Page<FraudCaseEntity> result =
        caseRepository.findAll(
            FraudCaseSpecifications.withFilters(
                status, customerId, transactionId, assignedTo, priority),
            pageable);
    return new FraudCasePageResponse(
        result.getContent().stream().map(this::toSummary).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  public FraudCasePageResponse list(int page, int size, String status) {
    return list(page, size, status, null, null, null, null);
  }

  public FraudCaseResponse get(String caseId) {
    FraudCaseEntity entity =
        caseRepository
            .findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + caseId));
    List<FraudCaseEventResponse> events =
        eventRepository.findByCaseIdOrderByCreatedAtAsc(caseId).stream()
            .map(this::toEvent)
            .toList();
    return toDetail(entity, events);
  }

  @Transactional
  public FraudCaseResponse assign(String caseId, FraudCaseAssignRequest request) {
    FraudCaseEntity entity = requireCase(caseId);
    entity.setAssignedTo(request.assignedTo());
    entity.setStatus("ASSIGNED");
    caseRepository.save(entity);
    appendEvent(caseId, "CASE_ASSIGNED", Map.of("assignedTo", request.assignedTo()));
    analystActionAuditService.record(
        "CASE_ASSIGN", "CASE", caseId, Map.of("assignedTo", request.assignedTo()));
    return get(caseId);
  }

  @Transactional
  public FraudCaseResponse updateStatus(String caseId, FraudCaseStatusRequest request) {
    String status = normalizeStatus(request.status());
    FraudCaseEntity entity = requireCase(caseId);
    entity.setStatus(status);
    if (request.closureReason() != null && !request.closureReason().isBlank()) {
      entity.setClosureReason(request.closureReason());
    }
    if ("CLOSED".equals(status)
        || "CONFIRMED_FRAUD".equals(status)
        || "FALSE_POSITIVE".equals(status)) {
      entity.setClosedAt(Instant.now());
    }
    caseRepository.save(entity);
    appendEvent(
        caseId,
        "STATUS_CHANGED",
        java.util.Map.of(
            "status",
            status,
            "closureReason",
            request.closureReason() == null ? "" : request.closureReason(),
            "notes",
            request.notes() == null ? "" : request.notes()));
    analystActionAuditService.record("CASE_STATUS", "CASE", caseId, Map.of("status", status));
    return get(caseId);
  }

  @Transactional
  public FraudCaseResponse addNote(String caseId, FraudCaseNoteRequest request) {
    requireCase(caseId);
    appendEvent(caseId, "CASE_NOTE", Map.of("note", request.note() == null ? "" : request.note()));
    analystActionAuditService.record("CASE_NOTE", "CASE", caseId, Map.of("note", request.note()));
    return get(caseId);
  }

  @Transactional
  public FraudCaseResponse escalate(String caseId) {
    FraudCaseEntity entity = requireCase(caseId);
    entity.setEscalationLevel(entity.getEscalationLevel() + 1);
    entity.setPriority("HIGH");
    entity.setSlaDueAt(Instant.now().plus(2, ChronoUnit.HOURS));
    caseRepository.save(entity);
    appendEvent(caseId, "CASE_ESCALATED", Map.of("level", entity.getEscalationLevel()));
    analystActionAuditService.record(
        "CASE_ESCALATE", "CASE", caseId, Map.of("level", entity.getEscalationLevel()));
    return get(caseId);
  }

  public List<FraudCaseSummaryResponse> byCustomer(String customerId, int limit) {
    return caseRepository
        .findByCustomerIdOrderByUpdatedAtDesc(customerId, PageRequest.of(0, Math.min(50, limit)))
        .stream()
        .map(this::toSummary)
        .toList();
  }

  private FraudCaseEntity requireCase(String caseId) {
    return caseRepository
        .findById(caseId)
        .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + caseId));
  }

  private void appendEvent(String caseId, String eventType, Map<String, Object> payload) {
    FraudCaseEventEntity event = new FraudCaseEventEntity();
    event.setEventId("EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    event.setCaseId(caseId);
    event.setEventType(eventType);
    event.setActorId(currentActor());
    try {
      event.setPayloadJson(objectMapper.writeValueAsString(payload == null ? Map.of() : payload));
    } catch (Exception ex) {
      event.setPayloadJson("{}");
    }
    eventRepository.save(event);
  }

  private String priorityFor(RiskAssessment assessment) {
    return priorityForScore(assessment.scoreBreakdown().finalScore().doubleValue());
  }

  private String priorityForScore(double score) {
    if (score >= 80) {
      return "CRITICAL";
    }
    if (score >= 60) {
      return "HIGH";
    }
    if (score >= 40) {
      return "MEDIUM";
    }
    return "LOW";
  }

  private void createCaseFromAudit(FraudDecisionAuditEntity audit) {
    FraudCaseEntity entity = new FraudCaseEntity();
    entity.setCaseId("CASE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    entity.setAssessmentId(audit.getAssessmentId());
    entity.setTransactionId(audit.getTransactionId());
    entity.setCustomerId(audit.getCustomerId());
    entity.setStatus("OPEN");
    entity.setPriority(
        priorityForScore(
            audit.getFinalScore() == null ? 0.0 : audit.getFinalScore().doubleValue()));
    entity.setEscalationLevel(0);
    Instant assessedAt = audit.getAssessedAt() == null ? Instant.now() : audit.getAssessedAt();
    entity.setSlaDueAt(assessedAt.plus(4, ChronoUnit.HOURS));
    entity.setCreatedAt(assessedAt);
    entity.setUpdatedAt(assessedAt);
    String category =
        audit.getMerchantCategory() == null || audit.getMerchantCategory().isBlank()
            ? "unknown category"
            : audit.getMerchantCategory();
    double amount = audit.getAmount() == null ? 0.0 : audit.getAmount().doubleValue();
    entity.setSummary(
        audit.getDecision()
            + " decision on "
            + category
            + " for $"
            + String.format("%.2f", amount));
    caseRepository.save(entity);
    appendEvent(
        entity.getCaseId(),
        "CASE_CREATED",
        Map.of("decision", audit.getDecision(), "source", "BACKFILL"));
  }

  private Object[] unwrapAggregateRow(Object[] row) {
    if (row == null || row.length == 0) {
      return new Object[] {0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L};
    }
    if (row.length == 1 && row[0] instanceof Object[] nested) {
      return nested;
    }
    return row;
  }

  private long toLong(Object value) {
    if (value == null) {
      return 0L;
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    return Long.parseLong(String.valueOf(value));
  }

  private String normalizeStatus(String status) {
    if (status == null || status.isBlank()) {
      throw new IllegalArgumentException("Case status is required");
    }
    String normalized = status.trim().toUpperCase();
    if (!VALID_STATUSES.contains(normalized)) {
      throw new IllegalArgumentException("Invalid case status: " + status);
    }
    return normalized;
  }

  private FraudCaseSummaryResponse toSummary(FraudCaseEntity entity) {
    return new FraudCaseSummaryResponse(
        entity.getCaseId(),
        entity.getAssessmentId(),
        entity.getTransactionId(),
        entity.getCustomerId(),
        entity.getStatus(),
        entity.getPriority(),
        entity.getAssignedTo(),
        entity.getSlaDueAt(),
        entity.getEscalationLevel(),
        entity.getSummary(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  private FraudCaseResponse toDetail(FraudCaseEntity entity, List<FraudCaseEventResponse> events) {
    return new FraudCaseResponse(
        entity.getCaseId(),
        entity.getAssessmentId(),
        entity.getTransactionId(),
        entity.getCustomerId(),
        entity.getStatus(),
        entity.getPriority(),
        entity.getAssignedTo(),
        entity.getSlaDueAt(),
        entity.getEscalationLevel(),
        entity.getClosureReason(),
        entity.getSummary(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getClosedAt(),
        events);
  }

  private FraudCaseEventResponse toEvent(FraudCaseEventEntity entity) {
    return new FraudCaseEventResponse(
        entity.getEventId(),
        entity.getEventType(),
        entity.getActorId(),
        readPayload(entity.getPayloadJson()),
        entity.getCreatedAt());
  }

  private Map<String, Object> readPayload(String json) {
    try {
      return json == null || json.isBlank()
          ? Map.of()
          : objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    } catch (Exception ex) {
      return Map.of();
    }
  }

  private String currentActor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication == null || authentication.getPrincipal() == null
        ? "system"
        : String.valueOf(authentication.getPrincipal());
  }
}
