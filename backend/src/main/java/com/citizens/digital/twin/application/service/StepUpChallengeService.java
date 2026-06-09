package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.api.dto.StepUpChallengeItemResponse;
import com.citizens.digital.twin.api.dto.StepUpChallengePageResponse;
import com.citizens.digital.twin.api.dto.StepUpChallengeRequest;
import com.citizens.digital.twin.api.dto.StepUpChallengeResponse;
import com.citizens.digital.twin.domain.model.Decision;
import com.citizens.digital.twin.domain.model.IdentityTwin;
import com.citizens.digital.twin.domain.model.RiskAssessment;
import com.citizens.digital.twin.domain.model.RiskSignal;
import com.citizens.digital.twin.domain.model.TransactionEvent;
import com.citizens.digital.twin.infrastructure.config.ScoringProperties;
import com.citizens.digital.twin.infrastructure.persistence.entity.FraudDecisionAuditEntity;
import com.citizens.digital.twin.infrastructure.persistence.entity.StepUpChallengeEntity;
import com.citizens.digital.twin.infrastructure.persistence.repository.StepUpChallengeJpaRepository;
import com.citizens.digital.twin.shared.exception.BusinessException;
import com.citizens.digital.twin.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StepUpChallengeService {
  private static final String STATUS_PENDING = "PENDING";
  private static final String STATUS_APPROVED = "APPROVED";
  private static final String STATUS_REJECTED = "REJECTED";
  private static final String STATUS_EXPIRED = "EXPIRED";

  private final StepUpChallengeJpaRepository repository;
  private final FraudDecisionAuditService auditService;
  private final IdentityTwinService identityTwinService;
  private final TwinSynchronizationService twinSynchronizationService;
  private final FraudCaseNoteService fraudCaseNoteService;
  private final FraudOutcomePublisher fraudOutcomePublisher;
  private final ScoringProperties scoringProperties;
  private final ObjectMapper objectMapper;

  public StepUpChallengeService(
      StepUpChallengeJpaRepository repository,
      FraudDecisionAuditService auditService,
      IdentityTwinService identityTwinService,
      TwinSynchronizationService twinSynchronizationService,
      FraudCaseNoteService fraudCaseNoteService,
      FraudOutcomePublisher fraudOutcomePublisher,
      ScoringProperties scoringProperties,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.auditService = auditService;
    this.identityTwinService = identityTwinService;
    this.twinSynchronizationService = twinSynchronizationService;
    this.fraudCaseNoteService = fraudCaseNoteService;
    this.fraudOutcomePublisher = fraudOutcomePublisher;
    this.scoringProperties = scoringProperties;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public Optional<StepUpChallengeEntity> createFromAssessment(
      TransactionEvent event, RiskAssessment assessment) {
    if (assessment.decision() != Decision.CHALLENGE) {
      return Optional.empty();
    }
    if (repository
        .findByAssessmentIdAndChallengeStatus(assessment.assessmentId(), STATUS_PENDING)
        .isPresent()) {
      return Optional.empty();
    }
    StepUpChallengeEntity entity =
        buildEntity(event, assessment, event.channel(), primaryReason(assessment));
    return Optional.of(repository.save(entity));
  }

  @Transactional
  public StepUpChallengeResponse create(StepUpChallengeRequest request) {
    FraudDecisionAuditEntity audit =
        auditService
            .findByAssessmentId(request.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
    if (repository
        .findByAssessmentIdAndChallengeStatus(request.assessmentId(), STATUS_PENDING)
        .isPresent()) {
      throw new BusinessException("A pending challenge already exists for this assessment");
    }
    TransactionEvent event = auditService.parseTransactionEvent(audit);
    StepUpChallengeEntity entity =
        buildEntity(
            event,
            audit,
            request.channel() == null || request.channel().isBlank()
                ? event.channel()
                : request.channel(),
            request.reason() == null || request.reason().isBlank()
                ? "Manual step-up challenge"
                : request.reason());
    StepUpChallengeEntity saved = repository.save(entity);
    fraudOutcomePublisher.publishStepUp(saved);
    return toResponse(saved, "Step-up challenge created for customer confirmation.");
  }

  public StepUpChallengePageResponse list(int page, int size, String status) {
    int safePage = Math.max(0, page);
    int safeSize = Math.min(200, Math.max(1, size));
    PageRequest pageable = PageRequest.of(safePage, safeSize);
    Page<StepUpChallengeEntity> result =
        status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)
            ? repository.findAllByOrderByCreatedAtDesc(pageable)
            : repository.findByChallengeStatusOrderByCreatedAtDesc(status.toUpperCase(), pageable);
    List<StepUpChallengeItemResponse> items =
        result.getContent().stream().map(this::toItem).toList();
    return new StepUpChallengePageResponse(
        items,
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  public StepUpChallengePageResponse queue(int page, int size) {
    return list(page, size, STATUS_PENDING);
  }

  public StepUpChallengeItemResponse get(String challengeId) {
    return toItem(loadChallenge(challengeId));
  }

  @Transactional
  public StepUpChallengeResponse approve(String challengeId) {
    StepUpChallengeEntity challenge = loadPendingChallenge(challengeId);
    FraudDecisionAuditEntity audit =
        auditService
            .findByAssessmentId(challenge.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
    TransactionEvent event = auditService.parseTransactionEvent(audit);
    IdentityTwin twin = identityTwinService.getOrCreateTwin(challenge.getCustomerId());
    twinSynchronizationService.synchronize(twin, event);
    auditService.updateDecision(challenge.getAssessmentId(), Decision.ALLOW.name());
    challenge.setChallengeStatus(STATUS_APPROVED);
    challenge.setApprovedAt(Instant.now());
    StepUpChallengeEntity saved = repository.save(challenge);
    fraudOutcomePublisher.publishStepUp(saved);
    return toResponse(saved, "Challenge approved. Twin baseline updated.");
  }

  @Transactional
  public StepUpChallengeResponse reject(String challengeId) {
    StepUpChallengeEntity challenge = loadPendingChallenge(challengeId);
    auditService.updateDecision(challenge.getAssessmentId(), Decision.BLOCK.name());
    challenge.setChallengeStatus(STATUS_REJECTED);
    challenge.setRejectedAt(Instant.now());
    StepUpChallengeEntity saved = repository.save(challenge);
    fraudCaseNoteService.saveAnalystNote(
        saved.getChallengeId(),
        saved.getAssessmentId(),
        saved.getCustomerId(),
        "analyst",
        "Step-up challenge rejected. Transaction marked as fraud.",
        Map.of("finalRiskScore", saved.getFinalRiskScore()));
    fraudOutcomePublisher.publishStepUp(saved);
    return toResponse(saved, "Challenge rejected. Transaction blocked.");
  }

  @Transactional
  public StepUpChallengeResponse expire(String challengeId) {
    StepUpChallengeEntity challenge = loadPendingChallenge(challengeId);
    challenge.setChallengeStatus(STATUS_EXPIRED);
    challenge.setExpiredAt(Instant.now());
    StepUpChallengeEntity saved = repository.save(challenge);
    fraudOutcomePublisher.publishStepUp(saved);
    return toResponse(saved, "Challenge expired.");
  }

  private StepUpChallengeEntity buildEntity(
      TransactionEvent event, RiskAssessment assessment, String channel, String reason) {
    StepUpChallengeEntity entity = new StepUpChallengeEntity();
    entity.setChallengeId(UUID.randomUUID().toString());
    entity.setAssessmentId(assessment.assessmentId());
    entity.setCustomerId(event.customerId());
    entity.setTransactionId(event.transactionId());
    entity.setChallengeType("OUT_OF_BAND");
    entity.setChallengeStatus(STATUS_PENDING);
    entity.setDeliveryChannel(channel == null || channel.isBlank() ? "WEB" : channel);
    entity.setReasonCode("STEP_UP_REQUIRED");
    entity.setReasonDescription(reason);
    entity.setRuleScore(assessment.scoreBreakdown().ruleScore());
    entity.setMlScore(assessment.scoreBreakdown().mlScore());
    entity.setFinalRiskScore(assessment.scoreBreakdown().finalScore());
    entity.setExplainabilityJson(writeExplainability(assessment));
    entity.setExpiresAt(
        Instant.now().plus(scoringProperties.challengeExpirationHours(), ChronoUnit.HOURS));
    entity.setCreatedAt(Instant.now());
    entity.setUpdatedAt(Instant.now());
    return entity;
  }

  private StepUpChallengeEntity buildEntity(
      TransactionEvent event, FraudDecisionAuditEntity audit, String channel, String reason) {
    StepUpChallengeEntity entity = new StepUpChallengeEntity();
    entity.setChallengeId(UUID.randomUUID().toString());
    entity.setAssessmentId(audit.getAssessmentId());
    entity.setCustomerId(audit.getCustomerId());
    entity.setTransactionId(audit.getTransactionId());
    entity.setChallengeType("OUT_OF_BAND");
    entity.setChallengeStatus(STATUS_PENDING);
    entity.setDeliveryChannel(channel == null || channel.isBlank() ? "WEB" : channel);
    entity.setReasonCode("STEP_UP_REQUIRED");
    entity.setReasonDescription(reason);
    entity.setRuleScore(audit.getFinalScore());
    entity.setMlScore(audit.getFinalScore());
    entity.setFinalRiskScore(audit.getFinalScore());
    entity.setExplainabilityJson(
        audit.getReasonCodesJson() == null ? "[]" : audit.getReasonCodesJson());
    entity.setExpiresAt(
        Instant.now().plus(scoringProperties.challengeExpirationHours(), ChronoUnit.HOURS));
    entity.setCreatedAt(Instant.now());
    entity.setUpdatedAt(Instant.now());
    return entity;
  }

  private StepUpChallengeEntity loadChallenge(String challengeId) {
    return repository
        .findById(challengeId)
        .orElseThrow(() -> new ResourceNotFoundException("Challenge not found"));
  }

  private StepUpChallengeEntity loadPendingChallenge(String challengeId) {
    StepUpChallengeEntity challenge = loadChallenge(challengeId);
    if (!STATUS_PENDING.equals(challenge.getChallengeStatus())) {
      throw new BusinessException("Challenge is not pending");
    }
    if (challenge.getExpiresAt().isBefore(Instant.now())) {
      throw new BusinessException("Challenge has expired");
    }
    return challenge;
  }

  private String primaryReason(RiskAssessment assessment) {
    return assessment.reasonCodes().stream()
        .map(RiskSignal::message)
        .filter(message -> message != null && !message.isBlank())
        .findFirst()
        .orElse("Step-up verification required");
  }

  private String writeExplainability(RiskAssessment assessment) {
    try {
      return objectMapper.writeValueAsString(assessment.reasonCodes());
    } catch (Exception ex) {
      return "[]";
    }
  }

  private StepUpChallengeItemResponse toItem(StepUpChallengeEntity entity) {
    return new StepUpChallengeItemResponse(
        entity.getChallengeId(),
        entity.getAssessmentId(),
        entity.getCustomerId(),
        entity.getTransactionId(),
        entity.getChallengeStatus(),
        entity.getDeliveryChannel(),
        entity.getReasonDescription(),
        entity.getFinalRiskScore(),
        entity.getExpiresAt(),
        entity.getCreatedAt());
  }

  private StepUpChallengeResponse toResponse(StepUpChallengeEntity entity, String message) {
    return new StepUpChallengeResponse(
        entity.getChallengeId(), entity.getChallengeStatus(), message, entity.getCreatedAt());
  }
}
