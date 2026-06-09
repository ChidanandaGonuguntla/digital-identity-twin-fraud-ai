package com.citizens.digital.twin.infrastructure.persistence.repository;

import com.citizens.digital.twin.infrastructure.persistence.entity.CustomerVelocityFeaturesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerVelocityFeaturesJpaRepository
    extends JpaRepository<CustomerVelocityFeaturesEntity, String> {}
