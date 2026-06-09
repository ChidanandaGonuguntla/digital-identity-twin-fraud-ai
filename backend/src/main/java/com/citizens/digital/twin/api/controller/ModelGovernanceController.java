package com.citizens.digital.twin.api.controller;

import com.citizens.digital.twin.api.dto.BiasFairnessReviewResponse;
import com.citizens.digital.twin.api.dto.DataDriftSummaryResponse;
import com.citizens.digital.twin.api.dto.ModelApprovalRequest;
import com.citizens.digital.twin.api.dto.ModelExplainabilityReportResponse;
import com.citizens.digital.twin.api.dto.ModelRegistryEntryResponse;
import com.citizens.digital.twin.api.dto.ModelRejectionRequest;
import com.citizens.digital.twin.application.service.AnalystActionAuditService;
import com.citizens.digital.twin.application.service.ModelGovernanceService;
import com.citizens.digital.twin.application.service.ModelRegistryService;
import com.citizens.digital.twin.domain.ml.ModelMetadata;
import com.citizens.digital.twin.infrastructure.security.SecurityRoles;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/models/governance")
@RequiredArgsConstructor
public class ModelGovernanceController {
  private final ModelGovernanceService governanceService;
  private final ModelRegistryService registryService;
  private final AnalystActionAuditService analystActionAuditService;

  @GetMapping("/registry")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public List<ModelRegistryEntryResponse> registry() {
    return governanceService.registry();
  }

  @GetMapping("/metadata")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public ModelMetadata metadata() {
    return governanceService.governanceMetadata();
  }

  @GetMapping("/data-drift")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public DataDriftSummaryResponse dataDrift(@RequestParam(defaultValue = "24") int hours) {
    return governanceService.dataDrift(hours);
  }

  @GetMapping("/bias-review")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public BiasFairnessReviewResponse biasReview() {
    return governanceService.biasReview();
  }

  @GetMapping("/explainability/{assessmentId}")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public ModelExplainabilityReportResponse explainability(@PathVariable String assessmentId) {
    return governanceService.explainability(assessmentId);
  }

  @PostMapping("/approve")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public ModelRegistryEntryResponse approve(@RequestBody ModelApprovalRequest request) {
    return governanceService.approve(request.modelVersion(), request.note());
  }

  @PostMapping("/reject")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public ModelRegistryEntryResponse reject(@RequestBody ModelRejectionRequest request) {
    return governanceService.reject(request.modelVersion(), request.reason());
  }

  @PostMapping("/submit")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public ModelRegistryEntryResponse submit(@RequestBody ModelApprovalRequest request) {
    return governanceService.submitForApproval(request.modelVersion());
  }

  @PostMapping("/activate/{modelVersion}")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public Map<String, Object> activate(@PathVariable String modelVersion) {
    Map<String, Object> status = registryService.activateVersion(modelVersion);
    analystActionAuditService.record("MODEL_ACTIVATE", "MODEL", modelVersion, status);
    return status;
  }

  @GetMapping(value = "/audit-report", produces = "text/csv")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.AUDITOR
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public ResponseEntity<String> auditReport() {
    String csv = governanceService.exportAuditReport();
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=model-governance-audit.csv")
        .contentType(MediaType.parseMediaType("text/csv"))
        .body(csv);
  }
}
