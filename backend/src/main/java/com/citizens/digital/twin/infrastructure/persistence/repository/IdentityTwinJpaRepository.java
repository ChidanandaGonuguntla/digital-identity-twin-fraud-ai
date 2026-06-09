package com.citizens.digital.twin.infrastructure.persistence.repository;

import com.citizens.digital.twin.infrastructure.persistence.entity.IdentityTwinEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentityTwinJpaRepository extends JpaRepository<IdentityTwinEntity, String> {}
