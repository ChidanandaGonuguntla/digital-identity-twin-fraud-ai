package com.citizens.digital.twin.infrastructure.persistence.repository;

import com.citizens.digital.twin.infrastructure.persistence.entity.FraudCaseEventEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudCaseEventJpaRepository extends JpaRepository<FraudCaseEventEntity, String> {
  List<FraudCaseEventEntity> findByCaseIdOrderByCreatedAtAsc(String caseId);
}
