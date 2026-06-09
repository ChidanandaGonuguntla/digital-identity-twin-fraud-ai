package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.domain.model.RiskAssessment;
import com.citizens.digital.twin.infrastructure.kafka.FraudKafkaEventPublisher;
import com.citizens.digital.twin.infrastructure.kafka.event.FraudAuditEvent;
import com.citizens.digital.twin.infrastructure.kafka.event.StepUpChallengeEvent;
import com.citizens.digital.twin.infrastructure.kafka.event.TwinDriftEvent;
import com.citizens.digital.twin.infrastructure.persistence.entity.StepUpChallengeEntity;
import com.citizens.digital.twin.infrastructure.persistence.entity.TwinDriftEventEntity;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class FraudOutcomePublisher {
  private final ObjectProvider<FraudKafkaEventPublisher> kafkaPublisher;

  public FraudOutcomePublisher(ObjectProvider<FraudKafkaEventPublisher> kafkaPublisher) {
    this.kafkaPublisher = kafkaPublisher;
  }

  public void publish(
      RiskAssessment assessment,
      Optional<StepUpChallengeEntity> challenge,
      Optional<TwinDriftEventEntity> drift) {
    kafkaPublisher.ifAvailable(
        publisher -> {
          publisher.publishAudit(toAuditEvent(assessment));
          challenge.ifPresent(c -> publisher.publishStepUp(toStepUpEvent(c)));
          drift.ifPresent(d -> publisher.publishDrift(toDriftEvent(d)));
        });
  }

  public void publishStepUp(StepUpChallengeEntity challenge) {
    kafkaPublisher.ifAvailable(publisher -> publisher.publishStepUp(toStepUpEvent(challenge)));
  }

  private FraudAuditEvent toAuditEvent(RiskAssessment assessment) {
    return new FraudAuditEvent(
        assessment.assessmentId(),
        assessment.transactionId(),
        assessment.customerId(),
        assessment.decision().name(),
        assessment.scoreBreakdown().finalScore(),
        assessment.modelVersion(),
        assessment.policyVersion(),
        assessment.latencyMs(),
        assessment.assessedAt());
  }

  private StepUpChallengeEvent toStepUpEvent(StepUpChallengeEntity challenge) {
    return new StepUpChallengeEvent(
        challenge.getChallengeId(),
        challenge.getAssessmentId(),
        challenge.getCustomerId(),
        challenge.getTransactionId(),
        challenge.getChallengeStatus(),
        challenge.getDeliveryChannel(),
        challenge.getReasonDescription(),
        challenge.getFinalRiskScore(),
        challenge.getExpiresAt(),
        challenge.getCreatedAt());
  }

  private TwinDriftEvent toDriftEvent(TwinDriftEventEntity drift) {
    return new TwinDriftEvent(
        drift.getDriftEventId(),
        drift.getAssessmentId(),
        drift.getCustomerId(),
        drift.getTransactionId(),
        drift.getDriftScore(),
        drift.getDriftThreshold(),
        drift.getDetectedAt());
  }
}
