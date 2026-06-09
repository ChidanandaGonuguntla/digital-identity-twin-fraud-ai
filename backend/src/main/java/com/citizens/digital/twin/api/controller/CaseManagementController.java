package com.citizens.digital.twin.api.controller;

import com.citizens.digital.twin.api.dto.*;
import com.citizens.digital.twin.application.service.FraudCaseManagementService;
import com.citizens.digital.twin.infrastructure.security.SecurityRoles;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cases")
@RequiredArgsConstructor
public class CaseManagementController {
  private final FraudCaseManagementService caseManagementService;

  @GetMapping("/summary")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public FraudCaseStatsResponse summary() {
    return caseManagementService.summary();
  }

  @PostMapping("/backfill")
  @RateLimiter(name = "fraudDecisions")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.FRAUD_MANAGER
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public FraudCaseBackfillResponse backfill(@RequestParam(defaultValue = "500") int limit) {
    return caseManagementService.backfill(limit);
  }

  @GetMapping
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public FraudCasePageResponse list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String customerId,
      @RequestParam(required = false) String transactionId,
      @RequestParam(required = false) String assignedTo,
      @RequestParam(required = false) String priority) {
    return caseManagementService.list(
        page, size, status, customerId, transactionId, assignedTo, priority);
  }

  @GetMapping("/{caseId}")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public FraudCaseResponse get(@PathVariable String caseId) {
    return caseManagementService.get(caseId);
  }

  @PostMapping("/{caseId}/assign")
  @RateLimiter(name = "fraudDecisions")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public FraudCaseResponse assign(
      @PathVariable String caseId, @RequestBody FraudCaseAssignRequest request) {
    return caseManagementService.assign(caseId, request);
  }

  @PostMapping("/{caseId}/status")
  @RateLimiter(name = "fraudDecisions")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public FraudCaseResponse updateStatus(
      @PathVariable String caseId, @RequestBody FraudCaseStatusRequest request) {
    return caseManagementService.updateStatus(caseId, request);
  }

  @PostMapping("/{caseId}/notes")
  @RateLimiter(name = "fraudDecisions")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public FraudCaseResponse addNote(
      @PathVariable String caseId, @RequestBody FraudCaseNoteRequest request) {
    return caseManagementService.addNote(caseId, request);
  }

  @PostMapping("/{caseId}/escalate")
  @RateLimiter(name = "fraudDecisions")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.FRAUD_MANAGER
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public FraudCaseResponse escalate(@PathVariable String caseId) {
    return caseManagementService.escalate(caseId);
  }
}
