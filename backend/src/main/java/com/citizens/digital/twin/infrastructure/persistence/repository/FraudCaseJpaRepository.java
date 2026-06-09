package com.citizens.digital.twin.infrastructure.persistence.repository;

import com.citizens.digital.twin.infrastructure.persistence.entity.FraudCaseEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface FraudCaseJpaRepository
    extends JpaRepository<FraudCaseEntity, String>, JpaSpecificationExecutor<FraudCaseEntity> {
  Page<FraudCaseEntity> findAllByOrderByUpdatedAtDesc(Pageable pageable);

  Page<FraudCaseEntity> findByStatusOrderByUpdatedAtDesc(String status, Pageable pageable);

  Page<FraudCaseEntity> findByAssignedToOrderByUpdatedAtDesc(String assignedTo, Pageable pageable);

  List<FraudCaseEntity> findByCustomerIdOrderByUpdatedAtDesc(String customerId, Pageable pageable);

  Optional<FraudCaseEntity> findByAssessmentId(String assessmentId);

  @Query(
      value =
          """
          SELECT COUNT(*),
                 COALESCE(SUM(CASE WHEN status NOT IN ('CLOSED', 'FALSE_POSITIVE', 'CONFIRMED_FRAUD') THEN 1 ELSE 0 END), 0),
                 COALESCE(SUM(CASE WHEN status = 'OPEN' THEN 1 ELSE 0 END), 0),
                 COALESCE(SUM(CASE WHEN status = 'ASSIGNED' THEN 1 ELSE 0 END), 0),
                 COALESCE(SUM(CASE WHEN status = 'IN_REVIEW' THEN 1 ELSE 0 END), 0),
                 COALESCE(SUM(CASE WHEN status = 'WAITING_CUSTOMER' THEN 1 ELSE 0 END), 0),
                 COALESCE(SUM(CASE WHEN status = 'CONFIRMED_FRAUD' THEN 1 ELSE 0 END), 0),
                 COALESCE(SUM(CASE WHEN status = 'FALSE_POSITIVE' THEN 1 ELSE 0 END), 0),
                 COALESCE(SUM(CASE WHEN status = 'CLOSED' THEN 1 ELSE 0 END), 0),
                 COALESCE(SUM(CASE WHEN priority IN ('CRITICAL', 'HIGH')
                   AND status NOT IN ('CLOSED', 'FALSE_POSITIVE', 'CONFIRMED_FRAUD') THEN 1 ELSE 0 END), 0),
                 COALESCE(SUM(CASE WHEN sla_due_at IS NOT NULL
                   AND sla_due_at < NOW()
                   AND status NOT IN ('CLOSED', 'FALSE_POSITIVE', 'CONFIRMED_FRAUD') THEN 1 ELSE 0 END), 0)
          FROM identity_twin.fraud_cases
          """,
      nativeQuery = true)
  Object[] summaryAggregate();
}
