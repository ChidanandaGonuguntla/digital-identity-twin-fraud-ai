package com.citizens.dti.service;

import com.citizens.dti.dto.FraudEvaluationRequest;
import com.citizens.dti.dto.StepUpChallengeResponse;
import com.citizens.dti.persistence.FraudStepUpChallengeEntity;
import com.citizens.dti.repository.FraudStepUpChallengeRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerPushChallengeService {

  private final FraudStepUpChallengeRepository repository;
  private final CustomerNotificationService notificationService;

  public CustomerPushChallengeService(
      FraudStepUpChallengeRepository repository, CustomerNotificationService notificationService) {
    this.repository = repository;
    this.notificationService = notificationService;
  }

  @Transactional
  public StepUpChallengeResponse createPushChallenge(
      FraudEvaluationRequest request,
      String reasonCode,
      BigDecimal ruleScore,
      BigDecimal mlScore,
      BigDecimal finalRiskScore) {
    UUID challengeId = UUID.randomUUID();

    FraudStepUpChallengeEntity challenge = new FraudStepUpChallengeEntity();
    challenge.setChallengeId(challengeId);
    challenge.setFraudEventId(request.fraudEventId());
    challenge.setCustomerId(request.customerId());
    challenge.setAccountId(request.accountId());
    challenge.setTransactionId(request.transactionId());
    challenge.setChallengeType("BANK_APP_PUSH");
    challenge.setChallengeStatus("PENDING");
    challenge.setDeliveryChannel("BANK_MOBILE_APP");
    challenge.setDestinationLabel("Registered mobile banking app");
    challenge.setReasonCode(reasonCode);
    challenge.setReasonDescription("Customer approval required for unusual transaction risk.");
    challenge.setRuleScore(ruleScore);
    challenge.setMlScore(mlScore);
    challenge.setFinalRiskScore(finalRiskScore);
    challenge.setExpiresAt(OffsetDateTime.now().plusMinutes(5));
    challenge.setCreatedAt(OffsetDateTime.now());

    repository.save(challenge);

    notificationService.sendTransactionApprovalPush(
        request.customerId(),
        challengeId,
        request.amount(),
        request.merchantName(),
        request.transactionId());

    return new StepUpChallengeResponse(
        challengeId,
        "BANK_APP_PUSH",
        "PENDING",
        "BANK_MOBILE_APP",
        "Registered mobile banking app",
        challenge.getExpiresAt(),
        reasonCode);
  }
}
