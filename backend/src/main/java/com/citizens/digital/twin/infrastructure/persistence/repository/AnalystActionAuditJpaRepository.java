package com.citizens.digital.twin.infrastructure.persistence.repository;

import com.citizens.digital.twin.infrastructure.persistence.entity.AnalystActionAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalystActionAuditJpaRepository
    extends JpaRepository<AnalystActionAuditEntity, String> {}
