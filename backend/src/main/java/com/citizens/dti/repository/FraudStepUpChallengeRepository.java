package com.citizens.dti.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.citizens.dti.persistence.FraudStepUpChallengeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudStepUpChallengeRepository
    extends JpaRepository<FraudStepUpChallengeEntity, UUID> {

  Optional<FraudStepUpChallengeEntity> findByFraudEventId(UUID fraudEventId);

  Optional<FraudStepUpChallengeEntity> findByTransactionId(String transactionId);

  List<FraudStepUpChallengeEntity> findTop25ByCustomerIdOrderByCreatedAtDesc(String customerId);

  List<FraudStepUpChallengeEntity> findByCustomerIdAndChallengeStatusOrderByCreatedAtDesc(
      String customerId, String challengeStatus);

  List<FraudStepUpChallengeEntity> findByChallengeStatusOrderByCreatedAtDesc(
      String challengeStatus);
}
