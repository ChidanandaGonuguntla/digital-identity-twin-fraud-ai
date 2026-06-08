package com.citizens.dti.api;

import com.citizens.dti.dto.StepUpApprovalRequest;
import com.citizens.dti.dto.StepUpApprovalResponse;
import com.citizens.dti.service.StepUpApprovalService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fraud/step-up")
public class StepUpChallengeController {

  private final StepUpApprovalService approvalService;

  public StepUpChallengeController(StepUpApprovalService approvalService) {
    this.approvalService = approvalService;
  }

  @PostMapping("/{challengeId}/decision")
  public ResponseEntity<StepUpApprovalResponse> approveOrDeny(
      @PathVariable UUID challengeId, @RequestBody StepUpApprovalRequest request) {
    return ResponseEntity.ok(approvalService.processCustomerDecision(challengeId, request));
  }
}
