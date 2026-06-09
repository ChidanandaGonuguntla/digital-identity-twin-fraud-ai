package com.citizens.digital.twin.infrastructure.persistence.repository;

import com.citizens.digital.twin.infrastructure.persistence.entity.FraudCaseNoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudCaseNoteJpaRepository extends JpaRepository<FraudCaseNoteEntity, String> {}
