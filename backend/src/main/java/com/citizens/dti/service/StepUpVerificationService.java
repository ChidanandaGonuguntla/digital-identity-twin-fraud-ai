package com.citizens.dti.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StepUpVerificationService {

  private final FraudStepUpChallengeRepository repository;
  private final OtpHashService otpHashService;

  @Transactional
  public StepUpVerificationResponse verify(UUID challengeId, String otp) {
    FraudStepUpChallengeEntity challenge =
        repository
            .findById(challengeId)
            .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));

    if (!"PENDING".equals(challenge.getChallengeStatus())) {
      return new StepUpVerificationResponse(
          challengeId, challenge.getChallengeStatus(), false, "Challenge is not pending");
    }

    if (challenge.getOtpExpiresAt().isBefore(OffsetDateTime.now())) {
      challenge.setChallengeStatus("EXPIRED");
      repository.save(challenge);

      return new StepUpVerificationResponse(challengeId, "EXPIRED", false, "OTP expired");
    }

    if (challenge.getAttemptCount() >= challenge.getMaxAttempts()) {
      challenge.setChallengeStatus("LOCKED");
      repository.save(challenge);

      return new StepUpVerificationResponse(
          challengeId, "LOCKED", false, "Maximum OTP attempts exceeded");
    }

    challenge.setAttemptCount(challenge.getAttemptCount() + 1);

    boolean matched = otpHashService.matches(otp, challenge.getOtpHash());

    if (matched) {
      challenge.setChallengeStatus("VERIFIED");
      challenge.setVerifiedAt(OffsetDateTime.now());
      repository.save(challenge);

      return new StepUpVerificationResponse(
          challengeId, "VERIFIED", true, "OTP verified successfully");
    }

    repository.save(challenge);

    return new StepUpVerificationResponse(challengeId, "PENDING", false, "Invalid OTP");
  }
}
