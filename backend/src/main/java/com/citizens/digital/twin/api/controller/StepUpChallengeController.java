package com.citizens.digital.twin.api.controller;

import com.citizens.digital.twin.api.dto.StepUpChallengeItemResponse;
import com.citizens.digital.twin.api.dto.StepUpChallengePageResponse;
import com.citizens.digital.twin.api.dto.StepUpChallengeRequest;
import com.citizens.digital.twin.api.dto.StepUpChallengeResponse;
import com.citizens.digital.twin.application.service.AnalystActionAuditService;
import com.citizens.digital.twin.application.service.StepUpChallengeService;
import com.citizens.digital.twin.infrastructure.security.SecurityRoles;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/challenges")
@RequiredArgsConstructor
public class StepUpChallengeController {

  private final StepUpChallengeService stepUpChallengeService;
  private final AnalystActionAuditService analystActionAuditService;

  @GetMapping
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public StepUpChallengePageResponse list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size,
      @RequestParam(required = false) String status) {
    return stepUpChallengeService.list(page, size, status);
  }

  @GetMapping("/queue")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public StepUpChallengePageResponse queue(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
    return stepUpChallengeService.queue(page, size);
  }

  @GetMapping("/{challengeId}")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public StepUpChallengeItemResponse get(@PathVariable String challengeId) {
    return stepUpChallengeService.get(challengeId);
  }

  @PostMapping
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public StepUpChallengeResponse create(@RequestBody StepUpChallengeRequest request) {
    StepUpChallengeResponse response = stepUpChallengeService.create(request);
    analystActionAuditService.record(
        "STEP_UP_CREATE",
        "CHALLENGE",
        response.challengeId(),
        Map.of("assessmentId", request.assessmentId(), "customerId", request.customerId()));
    return response;
  }

  @PostMapping("/{challengeId}/approve")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public StepUpChallengeResponse approve(@PathVariable String challengeId) {
    StepUpChallengeResponse response = stepUpChallengeService.approve(challengeId);
    analystActionAuditService.record(
        "STEP_UP_APPROVE", "CHALLENGE", challengeId, Map.of("status", response.status()));
    return response;
  }

  @PostMapping("/{challengeId}/reject")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public StepUpChallengeResponse reject(@PathVariable String challengeId) {
    StepUpChallengeResponse response = stepUpChallengeService.reject(challengeId);
    analystActionAuditService.record(
        "STEP_UP_REJECT", "CHALLENGE", challengeId, Map.of("status", response.status()));
    return response;
  }

  @PostMapping("/{challengeId}/expire")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public StepUpChallengeResponse expire(@PathVariable String challengeId) {
    StepUpChallengeResponse response = stepUpChallengeService.expire(challengeId);
    analystActionAuditService.record(
        "STEP_UP_EXPIRE", "CHALLENGE", challengeId, Map.of("status", response.status()));
    return response;
  }
}
