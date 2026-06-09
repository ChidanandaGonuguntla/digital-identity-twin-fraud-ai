package com.citizens.digital.twin.infrastructure.observability;

import com.citizens.digital.twin.domain.model.Decision;
import com.citizens.digital.twin.infrastructure.persistence.repository.StepUpChallengeJpaRepository;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;

@Service
public class FraudMetricsService {
  private final MeterRegistry meterRegistry;
  private final DistributionSummary decisionLatency;
  private final DistributionSummary modelScoreDistribution;
  private final AtomicReference<Double> twinDriftScore = new AtomicReference<>(0.0);
  private final AtomicReference<Double> modelDriftScore = new AtomicReference<>(0.0);
  private final AtomicReference<Double> modelDriftAlert = new AtomicReference<>(0.0);
  private final AtomicReference<Double> kafkaConsumerLag = new AtomicReference<>(0.0);
  private final StepUpChallengeJpaRepository stepUpChallengeRepository;

  public FraudMetricsService(
      MeterRegistry meterRegistry, StepUpChallengeJpaRepository stepUpChallengeRepository) {
    this.meterRegistry = meterRegistry;
    this.stepUpChallengeRepository = stepUpChallengeRepository;
    this.decisionLatency =
        DistributionSummary.builder(FraudMetrics.DECISION_LATENCY)
            .description("Fraud decision end-to-end latency in milliseconds")
            .baseUnit("milliseconds")
            .register(meterRegistry);
    this.modelScoreDistribution =
        DistributionSummary.builder(FraudMetrics.MODEL_SCORE)
            .description("Distribution of ML fraud model scores")
            .register(meterRegistry);
    Gauge.builder(FraudMetrics.TWIN_DRIFT_SCORE, twinDriftScore, AtomicReference::get)
        .description("Latest twin drift score")
        .register(meterRegistry);
    Gauge.builder(FraudMetrics.MODEL_DRIFT_SCORE, modelDriftScore, AtomicReference::get)
        .description("ML model score drift versus deployment baseline")
        .register(meterRegistry);
    Gauge.builder(FraudMetrics.MODEL_DRIFT_ALERT, modelDriftAlert, AtomicReference::get)
        .description("Model drift alert flag (1=alert, 0=normal)")
        .register(meterRegistry);
    Gauge.builder(FraudMetrics.KAFKA_CONSUMER_LAG, kafkaConsumerLag, AtomicReference::get)
        .description("Kafka consumer group lag for transaction events")
        .register(meterRegistry);
    Gauge.builder(
            FraudMetrics.STEP_UP_PENDING,
            this,
            service -> (double) service.stepUpChallengeRepository.countByChallengeStatus("PENDING"))
        .description("Pending step-up challenges")
        .register(meterRegistry);
  }

  public void recordDecision(Decision decision, long latencyMs, double mlScore, double driftScore) {
    meterRegistry.counter(FraudMetrics.DECISIONS_TOTAL, "decision", decision.name()).increment();
    if (decision == Decision.BLOCK) {
      meterRegistry.counter(FraudMetrics.DECISIONS_BLOCKED).increment();
    }
    if (decision == Decision.CHALLENGE) {
      meterRegistry.counter(FraudMetrics.DECISIONS_CHALLENGED).increment();
    }
    decisionLatency.record(latencyMs);
    modelScoreDistribution.record(mlScore);
    twinDriftScore.set(driftScore);
  }

  public void recordDuplicateEvent() {
    meterRegistry.counter(FraudMetrics.DUPLICATE_EVENTS).increment();
  }

  public void recordAuditWriteFailure() {
    meterRegistry.counter(FraudMetrics.AUDIT_WRITE_FAILURE).increment();
  }

  public void updateKafkaLag(long lag) {
    kafkaConsumerLag.set((double) lag);
  }

  public long currentKafkaConsumerLag() {
    return Math.round(kafkaConsumerLag.get());
  }

  public void updateModelDrift(double driftScore, boolean driftAlert) {
    modelDriftScore.set(driftScore);
    modelDriftAlert.set(driftAlert ? 1.0 : 0.0);
  }
}
