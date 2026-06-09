package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.api.dto.ModelLiveMetricsResponse;
import com.citizens.digital.twin.api.dto.PlatformOpsSummaryResponse;
import com.citizens.digital.twin.infrastructure.observability.FraudMetricsService;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class PlatformOpsService {
  private static final long LATENCY_SLO_MS = 300L;
  private static final long KAFKA_LAG_SLO = 1000L;
  private static final double DRIFT_SLO = 0.35;

  private final FraudDecisionAuditService auditService;
  private final ModelMonitoringService monitoringService;
  private final ModelDriftMonitorService driftMonitorService;
  private final FraudMetricsService fraudMetricsService;

  public PlatformOpsService(
      FraudDecisionAuditService auditService,
      ModelMonitoringService monitoringService,
      ModelDriftMonitorService driftMonitorService,
      FraudMetricsService fraudMetricsService) {
    this.auditService = auditService;
    this.monitoringService = monitoringService;
    this.driftMonitorService = driftMonitorService;
    this.fraudMetricsService = fraudMetricsService;
  }

  public PlatformOpsSummaryResponse summary() {
    long p95LatencyMs = auditService.summary().p95LatencyMs();
    long kafkaLag = fraudMetricsService.currentKafkaConsumerLag();
    double driftScore = driftMonitorService.currentDriftScore();
    boolean driftAlert = driftMonitorService.driftAlert();
    ModelLiveMetricsResponse live = monitoringService.liveMetrics();
    return new PlatformOpsSummaryResponse(
        p95LatencyMs,
        kafkaLag,
        driftScore,
        driftAlert,
        monitoringService.metadata().status(),
        live.scoredLastHour(),
        p95LatencyMs <= LATENCY_SLO_MS,
        kafkaLag < KAFKA_LAG_SLO,
        driftScore < DRIFT_SLO,
        Instant.now());
  }
}
