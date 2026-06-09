package com.citizens.digital.twin.api.controller;

import com.citizens.digital.twin.api.dto.AnalystFeedbackRequest;
import com.citizens.digital.twin.api.dto.AnalystFeedbackResponse;
import com.citizens.digital.twin.application.service.AnalystFeedbackService;
import com.citizens.digital.twin.infrastructure.security.SecurityRoles;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class FeedbackController {
  private final AnalystFeedbackService feedbackService;

  @PostMapping("/{assessmentId}")
  @RateLimiter(name = "fraudDecisions")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public AnalystFeedbackResponse submit(
      @PathVariable String assessmentId, @RequestBody AnalystFeedbackRequest request) {
    return feedbackService.submit(assessmentId, request);
  }

  @GetMapping
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "','"
          + SecurityRoles.AUDITOR
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public List<AnalystFeedbackResponse> list() {
    return feedbackService.listRecent();
  }

  @GetMapping("/export")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.FRAUD_MANAGER
          + "','"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public ResponseEntity<String> export() {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=analyst-feedback.csv")
        .contentType(MediaType.parseMediaType("text/csv"))
        .body(feedbackService.exportCsv());
  }
}
