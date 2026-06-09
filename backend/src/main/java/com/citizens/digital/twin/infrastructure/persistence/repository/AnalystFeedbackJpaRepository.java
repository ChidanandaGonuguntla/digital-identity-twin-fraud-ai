package com.citizens.digital.twin.infrastructure.persistence.repository;

import com.citizens.digital.twin.infrastructure.persistence.entity.AnalystFeedbackEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalystFeedbackJpaRepository extends JpaRepository<AnalystFeedbackEntity, String> {
  Optional<AnalystFeedbackEntity> findByAssessmentId(String assessmentId);

  List<AnalystFeedbackEntity> findAllByOrderByCreatedAtDesc();
}
