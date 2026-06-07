package com.aegis.digitaltwin.repository;

import com.aegis.digitaltwin.entity.CustomerIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerIdentityRepository extends JpaRepository<CustomerIdentity, Long> {
    Optional<CustomerIdentity> findByCustomerId(String customerId);
    boolean existsByCustomerId(String customerId);
}
