package com.citizens.digital.twin.infrastructure.persistence.repository;

import com.citizens.digital.twin.infrastructure.persistence.entity.TwinDriftEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TwinDriftEventJpaRepository extends JpaRepository<TwinDriftEventEntity, String> {
  java.util.List<TwinDriftEventEntity> findByCustomerIdOrderByDetectedAtDesc(
      String customerId, org.springframework.data.domain.Pageable pageable);
}
