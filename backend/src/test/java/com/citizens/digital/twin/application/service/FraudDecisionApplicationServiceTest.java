package com.citizens.digital.twin.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.citizens.digital.twin.api.dto.DecisionEvent;
import com.citizens.digital.twin.api.dto.DecisionEventFactory;
import com.citizens.digital.twin.api.dto.FraudDecisionRequest;
import com.citizens.digital.twin.api.dto.FraudDecisionResponse;
import com.citizens.digital.twin.application.orchestrator.RiskDecisionOrchestrator;
import com.citizens.digital.twin.domain.ml.ChampionChallengerOutcome;
import com.citizens.digital.twin.domain.ml.MlFraudModelService;
import com.citizens.digital.twin.domain.ml.MlFraudPrediction;
import com.citizens.digital.twin.domain.model.Decision;
import com.citizens.digital.twin.domain.model.IdentityTwin;
import com.citizens.digital.twin.domain.model.RiskAssessment;
import com.citizens.digital.twin.domain.model.RiskSignal;
import com.citizens.digital.twin.domain.model.RiskSignalType;
import com.citizens.digital.twin.domain.model.ScoreBreakdown;
import com.citizens.digital.twin.domain.model.Severity;
import com.citizens.digital.twin.domain.rule.RuleRiskEngine;
import com.citizens.digital.twin.domain.rule.RuleRiskEngine.RuleScore;
import com.citizens.digital.twin.domain.scoring.TwinDeviationScoringService;
import com.citizens.digital.twin.domain.scoring.TwinDeviationScoringService.TwinDeviationScore;
import com.citizens.digital.twin.infrastructure.observability.FraudMetricsService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FraudDecisionApplicationServiceTest {

  @Mock IdentityTwinService identityTwinService;
  @Mock RuleRiskEngine ruleRiskEngine;
  @Mock TwinDeviationScoringService twinDeviationScoringService;
  @Mock MlFraudModelService mlFraudModelService;
  @Mock ChampionChallengerScoringService championChallengerScoringService;
  @Mock FraudCaseManagementService fraudCaseManagementService;
  @Mock CustomerVelocityService customerVelocityService;
  @Mock RiskDecisionOrchestrator riskDecisionOrchestrator;
  @Mock FraudDecisionAuditService auditService;
  @Mock TwinSynchronizationService twinSynchronizationService;
  @Mock StepUpChallengeService stepUpChallengeService;
  @Mock TwinDriftService twinDriftService;
  @Mock ModelMetricsRecorderService modelMetricsRecorderService;
  @Mock ModelDriftMonitorService modelDriftMonitorService;
  @Mock FraudMetricsService fraudMetricsService;
  @Mock FraudOutcomePublisher fraudOutcomePublisher;
  @Mock DecisionEventFactory decisionEventFactory;
  @Mock DecisionPublisher decisionPublisher;

  @InjectMocks FraudDecisionApplicationService service;

  @Test
  void evaluate_runsCanonicalPipelineAndPublishesDecision() {
    FraudDecisionRequest request =
        new FraudDecisionRequest(
            "CUST-001",
            "TXN-001",
            100.0,
            "USD",
            "GROCERY",
            null,
            null,
            "dev-1",
            null,
            null,
            "MOBILE",
            null,
            35.0,
            -80.0,
            "US",
            null,
            Instant.parse("2026-06-08T12:00:00Z"),
            null);
    IdentityTwin twin = IdentityTwin.newTwin("CUST-001");
    RuleScore ruleScore = new RuleScore(BigDecimal.TEN, List.of());
    TwinDeviationScore twinScore =
        new TwinDeviationScore(BigDecimal.valueOf(12), List.of(), false);
    MlFraudPrediction mlPrediction =
        new MlFraudPrediction(
            BigDecimal.valueOf(0.2), BigDecimal.valueOf(20), "v1", List.of());
    ScoreBreakdown breakdown =
        new ScoreBreakdown(
            BigDecimal.valueOf(10),
            BigDecimal.valueOf(12),
            BigDecimal.valueOf(20),
            BigDecimal.ZERO,
            BigDecimal.valueOf(15));
    RiskAssessment assessment =
        new RiskAssessment(
            "asm-1",
            "TXN-001",
            "CUST-001",
            Decision.ALLOW,
            breakdown,
            List.of(
                new RiskSignal(
                    RiskSignalType.TWIN_DEVIATION,
                    "baseline",
                    "ok",
                    Severity.LOW,
                    BigDecimal.ONE,
                    "")),
            "v1",
            "p1",
            5L,
            Instant.parse("2026-06-08T12:00:01Z"));
    DecisionEvent event =
        new DecisionEvent(
            "asm-1",
            "TXN-001",
            "CUST-001",
            100.0,
            "GROCERY",
            "dev-1",
            35.0,
            -80.0,
            1L,
            15.0,
            "ALLOW",
            false,
            List.of(),
            List.of());

    when(auditService.findExistingResponse("TXN-001")).thenReturn(Optional.empty());
    when(identityTwinService.getOrCreateTwin("CUST-001")).thenReturn(twin);
    when(ruleRiskEngine.evaluate(eq(twin), any())).thenReturn(ruleScore);
    when(twinDeviationScoringService.score(eq(twin), any())).thenReturn(twinScore);
    when(mlFraudModelService.predict(eq(twin), any())).thenReturn(mlPrediction);
    when(championChallengerScoringService.score(eq(twin), any()))
        .thenReturn(new ChampionChallengerOutcome(mlPrediction, null, BigDecimal.ZERO, true));
    when(riskDecisionOrchestrator.decide(
            any(), eq(twin), eq(ruleScore), eq(twinScore), eq(mlPrediction), any(Long.class)))
        .thenReturn(assessment);
    when(decisionEventFactory.from(any(), eq(assessment))).thenReturn(event);
    when(twinDriftService.recordIfThreshold(any(), eq(assessment), eq(twin), eq(twinScore)))
        .thenReturn(Optional.empty());

    FraudDecisionResponse response = service.evaluate(request);

    InOrder order =
        inOrder(
            identityTwinService,
            ruleRiskEngine,
            twinDeviationScoringService,
            mlFraudModelService,
            riskDecisionOrchestrator,
            auditService,
            modelMetricsRecorderService,
            twinSynchronizationService,
            twinDriftService,
            decisionEventFactory,
            decisionPublisher,
            fraudOutcomePublisher);
    order.verify(identityTwinService).getOrCreateTwin("CUST-001");
    order.verify(auditService).save(any(), eq(assessment), eq(mlPrediction), eq(true), eq(false), any());
    order.verify(modelMetricsRecorderService).record(any(), eq(assessment), eq(mlPrediction));
    order.verify(twinSynchronizationService).synchronize(eq(twin), any());
    order.verify(decisionPublisher).publish(event);
    order.verify(fraudOutcomePublisher).publish(eq(assessment), eq(Optional.empty()), eq(Optional.empty()));

    assertThat(response.decision()).isEqualTo("ALLOW");
    assertThat(response.assessmentId()).isEqualTo("asm-1");
  }

  @Test
  void evaluate_createsChallengeWithoutTwinSyncWhenChallenged() {
    FraudDecisionRequest request =
        new FraudDecisionRequest(
            "CUST-002",
            "TXN-002",
            500.0,
            "USD",
            "ELECTRONICS",
            null,
            null,
            "dev-x",
            null,
            null,
            "WEB",
            null,
            1.0,
            103.0,
            "SG",
            null,
            Instant.parse("2026-06-08T12:00:00Z"),
            null);
    IdentityTwin twin = IdentityTwin.newTwin("CUST-002");
    RuleScore ruleScore = new RuleScore(BigDecimal.valueOf(50), List.of());
    TwinDeviationScore twinScore =
        new TwinDeviationScore(BigDecimal.valueOf(55), List.of(), false);
    MlFraudPrediction mlPrediction =
        new MlFraudPrediction(
            BigDecimal.valueOf(0.5), BigDecimal.valueOf(50), "v1", List.of());
    ScoreBreakdown breakdown =
        new ScoreBreakdown(
            BigDecimal.valueOf(50),
            BigDecimal.valueOf(55),
            BigDecimal.valueOf(50),
            BigDecimal.ZERO,
            BigDecimal.valueOf(52));
    RiskAssessment assessment =
        new RiskAssessment(
            "asm-2",
            "TXN-002",
            "CUST-002",
            Decision.CHALLENGE,
            breakdown,
            List.of(),
            "v1",
            "p1",
            8L,
            Instant.parse("2026-06-08T12:00:01Z"));
    DecisionEvent event =
        new DecisionEvent(
            "asm-2",
            "TXN-002",
            "CUST-002",
            500.0,
            "ELECTRONICS",
            "dev-x",
            1.0,
            103.0,
            1L,
            52.0,
            "CHALLENGE",
            false,
            List.of(),
            List.of());

    when(auditService.findExistingResponse("TXN-002")).thenReturn(Optional.empty());
    when(identityTwinService.getOrCreateTwin("CUST-002")).thenReturn(twin);
    when(ruleRiskEngine.evaluate(eq(twin), any())).thenReturn(ruleScore);
    when(twinDeviationScoringService.score(eq(twin), any())).thenReturn(twinScore);
    when(mlFraudModelService.predict(eq(twin), any())).thenReturn(mlPrediction);
    when(championChallengerScoringService.score(eq(twin), any()))
        .thenReturn(new ChampionChallengerOutcome(mlPrediction, null, BigDecimal.ZERO, true));
    when(riskDecisionOrchestrator.decide(
            any(), eq(twin), eq(ruleScore), eq(twinScore), eq(mlPrediction), any(Long.class)))
        .thenReturn(assessment);
    when(decisionEventFactory.from(any(), eq(assessment))).thenReturn(event);
    when(stepUpChallengeService.createFromAssessment(any(), eq(assessment)))
        .thenReturn(Optional.empty());
    when(twinDriftService.recordIfThreshold(any(), eq(assessment), eq(twin), eq(twinScore)))
        .thenReturn(Optional.empty());

    service.evaluate(request);

    verify(stepUpChallengeService).createFromAssessment(any(), eq(assessment));
    verify(twinSynchronizationService, never()).synchronize(any(), any());
  }
}
