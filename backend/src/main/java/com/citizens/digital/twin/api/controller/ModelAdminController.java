package com.citizens.digital.twin.api.controller;

import com.citizens.digital.twin.application.service.AnalystActionAuditService;
import com.citizens.digital.twin.application.service.ModelDriftMonitorService;
import com.citizens.digital.twin.application.service.ModelRegistryService;
import com.citizens.digital.twin.infrastructure.security.SecurityRoles;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/models/admin")
@RequiredArgsConstructor
public class ModelAdminController {
  private final ModelRegistryService modelRegistryService;
  private final ModelDriftMonitorService modelDriftMonitorService;
  private final AnalystActionAuditService analystActionAuditService;

  @GetMapping("/status")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public Map<String, Object> status() {
    Map<String, Object> status = modelRegistryService.status();
    status.put("driftBaseline", modelDriftMonitorService.baselineScore());
    status.put("driftScore", modelDriftMonitorService.currentDriftScore());
    status.put("driftAlert", modelDriftMonitorService.driftAlert());
    return status;
  }

  @GetMapping("/versions")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public List<String> versions() {
    return modelRegistryService.versions();
  }

  @PostMapping("/reload")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public Map<String, Object> reload() {
    Map<String, Object> status = modelRegistryService.reloadActive();
    analystActionAuditService.record(
        "MODEL_RELOAD", "MODEL", String.valueOf(status.get("activeVersion")), status);
    return status;
  }

  @PostMapping("/rollback")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public Map<String, Object> rollback() {
    Map<String, Object> status = modelRegistryService.rollback();
    analystActionAuditService.record(
        "MODEL_ROLLBACK", "MODEL", String.valueOf(status.get("activeVersion")), status);
    return status;
  }
}
