package com.citizens.dti.service;

import com.citizens.dti.dto.StepUpApprovalRequest;
import com.citizens.dti.dto.StepUpApprovalResponse;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.citizens.dti.persistence.FraudStepUpChallengeEntity;
import com.citizens.dti.repository.FraudStepUpChallengeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StepUpApprovalService {

  private final FraudStepUpChallengeRepository repository;

  public StepUpApprovalService(FraudStepUpChallengeRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public StepUpApprovalResponse processCustomerDecision(
      UUID challengeId, StepUpApprovalRequest request) {
    FraudStepUpChallengeEntity challenge =
        repository
            .findById(challengeId)
            .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));

    if (!challenge.getCustomerId().equals(request.customerId())) {
      return new StepUpApprovalResponse(challengeId, "FAILED", false, "Customer mismatch");
    }

    if (!"PENDING".equals(challenge.getChallengeStatus())) {
      return new StepUpApprovalResponse(
          challengeId, challenge.getChallengeStatus(), false, "Challenge is no longer pending");
    }

    if (challenge.getExpiresAt().isBefore(OffsetDateTime.now())) {
      challenge.setChallengeStatus("EXPIRED");
      repository.save(challenge);

      return new StepUpApprovalResponse(challengeId, "EXPIRED", false, "Approval request expired");
    }

    if ("APPROVE".equalsIgnoreCase(request.decision())) {
      challenge.setChallengeStatus("APPROVED");
      challenge.setApprovedAt(OffsetDateTime.now());
      repository.save(challenge);

      return new StepUpApprovalResponse(
          challengeId, "APPROVED", true, "Customer approved the transaction");
    }

    if ("DENY".equalsIgnoreCase(request.decision())) {
      challenge.setChallengeStatus("DENIED");
      challenge.setDeniedAt(OffsetDateTime.now());
      repository.save(challenge);

      return new StepUpApprovalResponse(
          challengeId, "DENIED", false, "Customer denied the transaction");
    }

    return new StepUpApprovalResponse(challengeId, "PENDING", false, "Invalid decision");
  }
}
