package com.citizens.digital.twin.api.controller;

import com.citizens.digital.twin.api.dto.ModelRegistryEntryResponse;
import com.citizens.digital.twin.application.service.AnalystActionAuditService;
import com.citizens.digital.twin.application.service.ChampionChallengerAnalyticsService;
import com.citizens.digital.twin.application.service.ModelGovernanceService;
import com.citizens.digital.twin.application.service.ModelRegistryService;
import com.citizens.digital.twin.infrastructure.security.SecurityRoles;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
public class ModelsController {
  private final ModelGovernanceService governanceService;
  private final ModelRegistryService registryService;
  private final AnalystActionAuditService analystActionAuditService;
  private final ChampionChallengerAnalyticsService championChallengerAnalyticsService;

  @GetMapping
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "')")
  public List<ModelRegistryEntryResponse> listModels() {
    return governanceService.registry();
  }

  @PostMapping("/{version}/activate")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public Map<String, Object> activate(@PathVariable String version) {
    Map<String, Object> status = registryService.activateVersion(version);
    analystActionAuditService.record("MODEL_ACTIVATE", "MODEL", version, status);
    return status;
  }

  @PostMapping("/{version}/retire")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public ModelRegistryEntryResponse retire(@PathVariable String version) {
    return governanceService.retire(version);
  }

  @PostMapping("/{version}/rollback")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public Map<String, Object> rollback(@PathVariable String version) {
    Map<String, Object> status = registryService.activateVersion(version);
    analystActionAuditService.record("MODEL_ROLLBACK", "MODEL", version, status);
    return status;
  }

  @GetMapping("/champion-challenger/summary")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "')")
  public com.citizens.digital.twin.api.dto.ChampionChallengerSummaryResponse
      championChallengerSummary(@RequestParam(defaultValue = "24") int hours) {
    return championChallengerAnalyticsService.summary(hours);
  }

  @PostMapping("/champion-challenger/promote")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public Map<String, Object> promoteChallenger() {
    Map<String, Object> status = registryService.promoteChallengerToChampion();
    analystActionAuditService.record("MODEL_PROMOTE_CHALLENGER", "MODEL", "challenger", status);
    return status;
  }
}
