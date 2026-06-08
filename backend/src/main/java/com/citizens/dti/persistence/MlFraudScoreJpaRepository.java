package com.citizens.dti.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MlFraudScoreJpaRepository extends JpaRepository<MlFraudScoreEntity, Long> {

  List<MlFraudScoreEntity> findByCustomerIdOrderByDecidedAtDesc(String customerId);

  List<MlFraudScoreEntity> findByFinalDecisionOrderByDecidedAtDesc(String finalDecision);
}
