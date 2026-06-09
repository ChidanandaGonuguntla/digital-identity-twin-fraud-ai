package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.api.dto.*;
import com.citizens.digital.twin.application.orchestrator.RiskDecisionOrchestrator;
import com.citizens.digital.twin.domain.ml.ChampionChallengerOutcome;
import com.citizens.digital.twin.domain.ml.MlFraudModelService;
import com.citizens.digital.twin.domain.ml.MlFraudPrediction;
import com.citizens.digital.twin.domain.model.*;
import com.citizens.digital.twin.domain.rule.RuleRiskEngine;
import com.citizens.digital.twin.domain.rule.RuleRiskEngine.RuleScore;
import com.citizens.digital.twin.domain.scoring.TwinDeviationScoringService;
import com.citizens.digital.twin.domain.scoring.TwinDeviationScoringService.TwinDeviationScore;
import com.citizens.digital.twin.infrastructure.observability.FraudMetricsService;
import com.citizens.digital.twin.infrastructure.persistence.entity.StepUpChallengeEntity;
import com.citizens.digital.twin.infrastructure.persistence.entity.TwinDriftEventEntity;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudDecisionApplicationService {
  private static final Logger log = LoggerFactory.getLogger(FraudDecisionApplicationService.class);

  private final IdentityTwinService identityTwinService;
  private final RuleRiskEngine ruleRiskEngine;
  private final TwinDeviationScoringService twinDeviationScoringService;
  private final MlFraudModelService mlFraudModelService;
  private final ChampionChallengerScoringService championChallengerScoringService;
  private final RiskDecisionOrchestrator riskDecisionOrchestrator;
  private final FraudDecisionAuditService auditService;
  private final TwinSynchronizationService twinSynchronizationService;
  private final StepUpChallengeService stepUpChallengeService;
  private final TwinDriftService twinDriftService;
  private final ModelMetricsRecorderService modelMetricsRecorderService;
  private final ModelDriftMonitorService modelDriftMonitorService;
  private final FraudOutcomePublisher fraudOutcomePublisher;
  private final DecisionEventFactory decisionEventFactory;
  private final DecisionPublisher decisionPublisher;
  private final FraudMetricsService fraudMetricsService;
  private final FraudCaseManagementService fraudCaseManagementService;
  private final CustomerVelocityService customerVelocityService;

  public FraudDecisionApplicationService(
      IdentityTwinService identityTwinService,
      RuleRiskEngine ruleRiskEngine,
      TwinDeviationScoringService twinDeviationScoringService,
      MlFraudModelService mlFraudModelService,
      ChampionChallengerScoringService championChallengerScoringService,
      RiskDecisionOrchestrator riskDecisionOrchestrator,
      FraudDecisionAuditService auditService,
      TwinSynchronizationService twinSynchronizationService,
      StepUpChallengeService stepUpChallengeService,
      TwinDriftService twinDriftService,
      ModelMetricsRecorderService modelMetricsRecorderService,
      ModelDriftMonitorService modelDriftMonitorService,
      FraudOutcomePublisher fraudOutcomePublisher,
      DecisionEventFactory decisionEventFactory,
      DecisionPublisher decisionPublisher,
      FraudMetricsService fraudMetricsService,
      FraudCaseManagementService fraudCaseManagementService,
      CustomerVelocityService customerVelocityService) {
    this.identityTwinService = identityTwinService;
    this.ruleRiskEngine = ruleRiskEngine;
    this.twinDeviationScoringService = twinDeviationScoringService;
    this.mlFraudModelService = mlFraudModelService;
    this.championChallengerScoringService = championChallengerScoringService;
    this.riskDecisionOrchestrator = riskDecisionOrchestrator;
    this.auditService = auditService;
    this.twinSynchronizationService = twinSynchronizationService;
    this.stepUpChallengeService = stepUpChallengeService;
    this.twinDriftService = twinDriftService;
    this.modelMetricsRecorderService = modelMetricsRecorderService;
    this.modelDriftMonitorService = modelDriftMonitorService;
    this.fraudOutcomePublisher = fraudOutcomePublisher;
    this.decisionEventFactory = decisionEventFactory;
    this.decisionPublisher = decisionPublisher;
    this.fraudMetricsService = fraudMetricsService;
    this.fraudCaseManagementService = fraudCaseManagementService;
    this.customerVelocityService = customerVelocityService;
  }

  @Transactional
  public FraudDecisionResponse evaluate(FraudDecisionRequest request) {
    Optional<FraudDecisionResponse> existing =
        auditService.findExistingResponse(request.transactionId());
    if (existing.isPresent()) {
      fraudMetricsService.recordDuplicateEvent();
      log.info(
          "Duplicate transaction skipped transactionId={} assessmentId={}",
          request.transactionId(),
          existing.get().assessmentId());
      return existing.get();
    }

    long startedAt = System.currentTimeMillis();
    TransactionEvent event = toTransactionEvent(request);
    IdentityTwin twin = identityTwinService.getOrCreateTwin(event.customerId());
    RuleScore ruleScore = ruleRiskEngine.evaluate(twin, event);
    TwinDeviationScore twinScore = twinDeviationScoringService.score(twin, event);
    ChampionChallengerOutcome championChallenger =
        championChallengerScoringService.score(twin, event);
    MlFraudPrediction mlPrediction = championChallenger.champion();
    long latencyMs = System.currentTimeMillis() - startedAt;
    RiskAssessment assessment =
        riskDecisionOrchestrator.decide(event, twin, ruleScore, twinScore, mlPrediction, latencyMs);
    boolean twinUpdated = assessment.decision() == Decision.ALLOW;
    boolean challenged = assessment.decision() == Decision.CHALLENGE;
    Optional<StepUpChallengeEntity> challenge = Optional.empty();
    if (twinUpdated) {
      twinSynchronizationService.synchronize(twin, event);
    } else if (challenged) {
      challenge = stepUpChallengeService.createFromAssessment(event, assessment);
    }
    try {
      auditService.save(
          event, assessment, mlPrediction, twinUpdated, challenged, championChallenger);
    } catch (DataIntegrityViolationException ex) {
      fraudMetricsService.recordAuditWriteFailure();
      Optional<FraudDecisionResponse> raced =
          auditService.findExistingResponse(request.transactionId());
      if (raced.isPresent()) {
        fraudMetricsService.recordDuplicateEvent();
        return raced.get();
      }
      throw ex;
    }
    fraudCaseManagementService.autoCreateFromAssessment(event, assessment);
    customerVelocityService.recordTransaction(twin, event, assessment.decision());
    modelMetricsRecorderService.record(event, assessment, mlPrediction);
    Optional<TwinDriftEventEntity> drift =
        twinDriftService.recordIfThreshold(event, assessment, twin, twinScore);
    DecisionEvent decisionEvent = decisionEventFactory.from(event, assessment);
    decisionPublisher.publish(decisionEvent);
    fraudOutcomePublisher.publish(assessment, challenge, drift);
    fraudMetricsService.recordDecision(
        assessment.decision(),
        latencyMs,
        mlPrediction.score().doubleValue(),
        modelDriftMonitorService.currentDriftScore());
    log.info(
        "Fraud decision completed transactionId={} assessmentId={} decision={} latencyMs={}",
        request.transactionId(),
        assessment.assessmentId(),
        assessment.decision(),
        latencyMs);
    return FraudDecisionResponse.from(assessment, decisionEvent);
  }

  private TransactionEvent toTransactionEvent(FraudDecisionRequest r) {
    return new TransactionEvent(
        r.customerId(),
        r.transactionId(),
        r.amount(),
        r.currency(),
        r.merchantCategory(),
        r.merchantName(),
        r.merchantId(),
        r.deviceId(),
        r.ipAddress(),
        r.userAgent(),
        r.channel(),
        r.paymentInstrumentId(),
        r.latitude(),
        r.longitude(),
        r.countryCode(),
        r.city(),
        r.timestamp(),
        r.metadata() == null ? Map.of() : r.metadata());
  }
}
