package com.citizens.digital.twin.api.controller;

import com.citizens.digital.twin.api.dto.ModelDriftPointResponse;
import com.citizens.digital.twin.api.dto.ModelLiveMetricsResponse;
import com.citizens.digital.twin.api.dto.ModelQualityMetricsResponse;
import com.citizens.digital.twin.application.service.ModelMonitoringService;
import com.citizens.digital.twin.domain.ml.ModelMetadata;
import com.citizens.digital.twin.infrastructure.security.SecurityRoles;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
public class ModelMonitoringController {

  private final ModelMonitoringService modelMonitoringService;

  @GetMapping("/fraud-risk/metadata")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public ModelMetadata metadata() {
    return modelMonitoringService.metadata();
  }

  @GetMapping("/fraud-risk/health")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public Map<String, Object> health() {
    return modelMonitoringService.health();
  }

  @GetMapping("/fraud-risk/live-metrics")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public ModelLiveMetricsResponse liveMetrics() {
    return modelMonitoringService.liveMetrics();
  }

  @GetMapping("/fraud-risk/drift-trend")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public List<ModelDriftPointResponse> driftTrend(@RequestParam(defaultValue = "24") int hours) {
    return modelMonitoringService.driftTrend(hours);
  }

  @GetMapping("/fraud-risk/quality-metrics")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public ModelQualityMetricsResponse qualityMetrics() {
    return modelMonitoringService.qualityMetrics();
  }
}
