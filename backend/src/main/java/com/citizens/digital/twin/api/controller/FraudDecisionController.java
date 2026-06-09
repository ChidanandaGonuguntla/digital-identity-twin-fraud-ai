package com.citizens.digital.twin.api.controller;

import com.citizens.digital.twin.api.dto.FraudDecisionRequest;
import com.citizens.digital.twin.api.dto.FraudDecisionResponse;
import com.citizens.digital.twin.application.service.FraudDecisionApplicationService;
import com.citizens.digital.twin.infrastructure.security.SecurityRoles;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fraud")
@RequiredArgsConstructor
public class FraudDecisionController {

  private final FraudDecisionApplicationService fraudDecisionApplicationService;

  @PostMapping("/decisions")
  @RateLimiter(name = "fraudDecisions")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public ResponseEntity<FraudDecisionResponse> decide(
      @Valid @RequestBody FraudDecisionRequest request) {
    return ResponseEntity.ok(fraudDecisionApplicationService.evaluate(request));
  }

  @PostMapping("/score")
  @RateLimiter(name = "fraudDecisions")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public ResponseEntity<FraudDecisionResponse> score(
      @Valid @RequestBody FraudDecisionRequest request) {
    return decide(request);
  }
}
