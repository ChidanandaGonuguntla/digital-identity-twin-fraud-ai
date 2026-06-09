package com.citizens.digital.twin.api.controller;

import com.citizens.digital.twin.api.dto.*;
import com.citizens.digital.twin.application.service.DecisionNarrativeService;
import com.citizens.digital.twin.application.service.FraudDecisionAuditService;
import com.citizens.digital.twin.infrastructure.security.SecurityRoles;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {
  private final FraudDecisionAuditService service;
  private final DecisionNarrativeService narrativeService;

  public AuditController(
      FraudDecisionAuditService service, DecisionNarrativeService narrativeService) {
    this.service = service;
    this.narrativeService = narrativeService;
  }

  @GetMapping("/decisions/recent")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.AUDITOR
          + "','"
          + SecurityRoles.ADMIN
          + "','"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "')")
  public AuditPageResponse recent() {
    return service.page(0, 50, null, null, null, null, null, null, null);
  }

  @GetMapping("/decisions/summary")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.AUDITOR
          + "','"
          + SecurityRoles.ADMIN
          + "','"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "')")
  public AuditSummaryResponse summary() {
    return service.summary();
  }

  @GetMapping("/decisions")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.AUDITOR
          + "','"
          + SecurityRoles.ADMIN
          + "','"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "')")
  public AuditPageResponse search(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size,
      @RequestParam(required = false) String decision,
      @RequestParam(required = false) String customerId,
      @RequestParam(required = false) String transactionId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to,
      @RequestParam(required = false) Double minScore,
      @RequestParam(required = false) Double maxScore) {
    return service.page(
        page, size, decision, customerId, transactionId, from, to, minScore, maxScore);
  }

  @GetMapping("/decisions/{assessmentId}")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.AUDITOR
          + "','"
          + SecurityRoles.ADMIN
          + "','"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "')")
  public FraudDecisionAuditDetailResponse detail(@PathVariable String assessmentId) {
    return service.getDetail(assessmentId);
  }

  @GetMapping("/decisions/{assessmentId}/narrative")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.AUDITOR
          + "','"
          + SecurityRoles.ADMIN
          + "','"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "')")
  public DecisionNarrativeResponse narrative(@PathVariable String assessmentId) {
    return narrativeService.build(assessmentId);
  }

  @GetMapping("/customers/{customerId}")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.AUDITOR
          + "','"
          + SecurityRoles.ADMIN
          + "','"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "')")
  public AuditPageResponse byCustomer(
      @PathVariable String customerId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return service.byCustomer(customerId, page, size);
  }

  @GetMapping("/transactions/{transactionId}")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.AUDITOR
          + "','"
          + SecurityRoles.ADMIN
          + "','"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "')")
  public AuditPageResponse byTransaction(
      @PathVariable String transactionId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return service.byTransaction(transactionId, page, size);
  }

  @GetMapping("/decisions/trends")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.AUDITOR
          + "','"
          + SecurityRoles.ADMIN
          + "','"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "')")
  public List<AuditTrendPointResponse> trends(@RequestParam(defaultValue = "24") int hours) {
    return service.trends(hours);
  }

  @GetMapping("/decisions/score-distribution")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.AUDITOR
          + "','"
          + SecurityRoles.ADMIN
          + "','"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "')")
  public List<AuditScoreBucketResponse> scoreDistribution() {
    return service.scoreDistribution();
  }

  @GetMapping("/decisions/reason-leaderboard")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.AUDITOR
          + "','"
          + SecurityRoles.ADMIN
          + "','"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "')")
  public List<AuditReasonLeaderboardItem> reasonLeaderboard() {
    return service.reasonLeaderboard();
  }

  @GetMapping(value = "/decisions/export", produces = "text/csv")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.AUDITOR
          + "','"
          + SecurityRoles.ADMIN
          + "','"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "','"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "')")
  public ResponseEntity<String> exportDecisions(
      @RequestParam(required = false) String decision,
      @RequestParam(required = false) String customerId,
      @RequestParam(required = false) String transactionId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to,
      @RequestParam(required = false) Double minScore,
      @RequestParam(required = false) Double maxScore) {
    String csv =
        service.exportDecisionsCsv(
            decision, customerId, transactionId, from, to, minScore, maxScore);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fraud-decisions-audit.csv")
        .contentType(MediaType.parseMediaType("text/csv"))
        .body(csv);
  }
}
