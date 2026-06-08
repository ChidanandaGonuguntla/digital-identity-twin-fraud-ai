package com.citizens.dti.service;

import com.citizens.dti.model.Decision;
import com.citizens.dti.model.IdentityTwin;
import com.citizens.dti.model.RiskAssessment;
import com.citizens.dti.model.TransactionEvent;
import com.citizens.dti.persistence.DecisionRecordService;
import com.citizens.dti.repository.IdentityTwinRepository;
import com.citizens.dti.twin.DeviationScoringEngine;
import com.citizens.dti.twin.TwinSynchronizationService;
import com.citizens.dti.web.DecisionPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FraudDetectionService {

  private final IdentityTwinRepository repository;
  private final DeviationScoringEngine scoringEngine;
  private final TwinSynchronizationService synchronizationService;
  private final DecisionPublisher decisionPublisher;
  private final DecisionRecordService decisionRecordService;

  public RiskAssessment assess(TransactionEvent event) {
    long start = System.currentTimeMillis();

    IdentityTwin twin = repository.findOrCreate(event.customerId());
    RiskAssessment assessment = scoringEngine.score(twin, event);

    // Push the decided transaction to the live console stream.
    decisionPublisher.publish(event, assessment);

    // Trusted behavior updates and persists the twin; blocked events never train it.
    if (assessment.decision() != Decision.BLOCK) {
      synchronizationService.synchronize(twin, event);
      repository.save(twin);
    }

    int totalLatencyMs = (int) (System.currentTimeMillis() - start);
    decisionRecordService.record(event, assessment, totalLatencyMs);

    return assessment;
  }

  /** Bootstrap a twin with known-good history (no scoring), then persist it. */
  public void seed(TransactionEvent event) {
    IdentityTwin twin = repository.findOrCreate(event.customerId());
    synchronizationService.synchronize(twin, event);
    repository.save(twin);
  }
}
