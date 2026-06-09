package com.citizens.digital.twin.infrastructure.persistence.repository;

import com.citizens.digital.twin.infrastructure.persistence.entity.StepUpChallengeEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StepUpChallengeJpaRepository extends JpaRepository<StepUpChallengeEntity, String> {
  Page<StepUpChallengeEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

  Page<StepUpChallengeEntity> findByChallengeStatusOrderByCreatedAtDesc(
      String challengeStatus, Pageable pageable);

  Optional<StepUpChallengeEntity> findByAssessmentIdAndChallengeStatus(
      String assessmentId, String challengeStatus);

  long countByChallengeStatus(String challengeStatus);

  java.util.List<StepUpChallengeEntity> findByCustomerIdOrderByCreatedAtDesc(
      String customerId, org.springframework.data.domain.Pageable pageable);
}
