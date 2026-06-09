package com.citizens.digital.twin.infrastructure.observability;

import com.citizens.digital.twin.application.service.ModelDriftMonitorService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ModelDriftMetricsExporter {
  private final ModelDriftMonitorService driftMonitorService;
  private final FraudMetricsService fraudMetricsService;

  public ModelDriftMetricsExporter(
      ModelDriftMonitorService driftMonitorService, FraudMetricsService fraudMetricsService) {
    this.driftMonitorService = driftMonitorService;
    this.fraudMetricsService = fraudMetricsService;
  }

  @Scheduled(fixedDelayString = "${fraud.observability.model-drift-interval-ms:30000}")
  public void exportModelDrift() {
    fraudMetricsService.updateModelDrift(
        driftMonitorService.currentDriftScore(), driftMonitorService.driftAlert());
  }
}
