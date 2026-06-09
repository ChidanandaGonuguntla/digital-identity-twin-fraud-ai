package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.api.dto.*;
import com.citizens.digital.twin.infrastructure.persistence.entity.FraudDecisionAuditEntity;
import com.citizens.digital.twin.infrastructure.persistence.entity.StepUpChallengeEntity;
import com.citizens.digital.twin.infrastructure.persistence.entity.TwinDriftEventEntity;
import com.citizens.digital.twin.infrastructure.persistence.repository.AnalystFeedbackJpaRepository;
import com.citizens.digital.twin.infrastructure.persistence.repository.FraudDecisionAuditJpaRepository;
import com.citizens.digital.twin.infrastructure.persistence.repository.StepUpChallengeJpaRepository;
import com.citizens.digital.twin.infrastructure.persistence.repository.TwinDriftEventJpaRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class Customer360TimelineService {
  private final TwinExplorerService twinExplorerService;
  private final CustomerVelocityService velocityService;
  private final FraudDecisionAuditJpaRepository auditRepository;
  private final TwinDriftEventJpaRepository driftRepository;
  private final StepUpChallengeJpaRepository challengeRepository;
  private final FraudCaseManagementService caseManagementService;
  private final AnalystFeedbackJpaRepository feedbackRepository;

  public Customer360TimelineService(
      TwinExplorerService twinExplorerService,
      CustomerVelocityService velocityService,
      FraudDecisionAuditJpaRepository auditRepository,
      TwinDriftEventJpaRepository driftRepository,
      StepUpChallengeJpaRepository challengeRepository,
      FraudCaseManagementService caseManagementService,
      AnalystFeedbackJpaRepository feedbackRepository) {
    this.twinExplorerService = twinExplorerService;
    this.velocityService = velocityService;
    this.auditRepository = auditRepository;
    this.driftRepository = driftRepository;
    this.challengeRepository = challengeRepository;
    this.caseManagementService = caseManagementService;
    this.feedbackRepository = feedbackRepository;
  }

  public Customer360TimelineResponse timeline(String customerId) {
    String resolved = customerId == null ? "" : customerId.trim().toUpperCase();
    TwinExplorerResponse profile = twinExplorerService.explore(resolved);
    CustomerVelocityResponse velocity = velocityService.get(resolved);
    List<Customer360TimelineEvent> events = new ArrayList<>();

    auditRepository
        .findByCustomerIdOrderByAssessedAtDesc(resolved, PageRequest.of(0, 25))
        .getContent()
        .forEach(a -> events.add(decisionEvent(a)));

    driftRepository
        .findByCustomerIdOrderByDetectedAtDesc(resolved, PageRequest.of(0, 15))
        .forEach(d -> events.add(driftEvent(d)));

    challengeRepository
        .findByCustomerIdOrderByCreatedAtDesc(resolved, PageRequest.of(0, 15))
        .forEach(c -> events.add(stepUpEvent(c)));

    caseManagementService.byCustomer(resolved, 15).stream()
        .map(
            c ->
                new Customer360TimelineEvent(
                    "FRAUD_CASE",
                    c.caseId(),
                    "Case " + c.status(),
                    c.summary(),
                    c.updatedAt(),
                    Map.of(
                        "priority", c.priority(),
                        "assignedTo", c.assignedTo() == null ? "" : c.assignedTo(),
                        "transactionId", c.transactionId())))
        .forEach(events::add);

    feedbackRepository.findAll().stream()
        .filter(f -> resolved.equals(f.getCustomerId()))
        .limit(15)
        .map(
            f ->
                new Customer360TimelineEvent(
                    "ANALYST_FEEDBACK",
                    f.getFeedbackId(),
                    f.getOutcome(),
                    f.getNotes(),
                    f.getCreatedAt(),
                    Map.of("assessmentId", f.getAssessmentId())))
        .forEach(events::add);

    events.sort(Comparator.comparing(Customer360TimelineEvent::occurredAt).reversed());
    return new Customer360TimelineResponse(
        resolved, profile, velocity, events.stream().limit(50).toList());
  }

  private Customer360TimelineEvent decisionEvent(FraudDecisionAuditEntity entity) {
    return new Customer360TimelineEvent(
        "DECISION",
        entity.getAssessmentId(),
        entity.getDecision() + " · score " + entity.getFinalScore(),
        entity.getFinalDecisionReason(),
        entity.getAssessedAt(),
        Map.of(
            "transactionId", entity.getTransactionId(),
            "amount", entity.getAmount() == null ? 0.0 : entity.getAmount().doubleValue(),
            "merchantCategory",
                entity.getMerchantCategory() == null ? "" : entity.getMerchantCategory(),
            "championScore",
                entity.getChampionScore() == null ? 0.0 : entity.getChampionScore().doubleValue(),
            "challengerScore",
                entity.getChallengerScore() == null
                    ? 0.0
                    : entity.getChallengerScore().doubleValue()));
  }

  private Customer360TimelineEvent driftEvent(TwinDriftEventEntity entity) {
    return new Customer360TimelineEvent(
        "TWIN_DRIFT",
        entity.getDriftEventId(),
        "Twin drift detected",
        "Drift score " + entity.getDriftScore(),
        entity.getDetectedAt(),
        Map.of(
            "transactionId", entity.getTransactionId(),
            "assessmentId", entity.getAssessmentId(),
            "driftScore", entity.getDriftScore().doubleValue()));
  }

  private Customer360TimelineEvent stepUpEvent(StepUpChallengeEntity entity) {
    return new Customer360TimelineEvent(
        "STEP_UP",
        entity.getChallengeId(),
        "Step-up " + entity.getChallengeStatus(),
        entity.getReasonDescription() == null
            ? entity.getReasonCode()
            : entity.getReasonDescription(),
        entity.getCreatedAt(),
        Map.of(
            "assessmentId", entity.getAssessmentId(),
            "transactionId", entity.getTransactionId(),
            "status", entity.getChallengeStatus()));
  }
}
