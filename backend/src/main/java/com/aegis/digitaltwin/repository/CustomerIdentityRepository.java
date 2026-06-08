package com.aegis.digitaltwin.repository;

import com.aegis.digitaltwin.entity.CustomerIdentity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerIdentityRepository extends JpaRepository<CustomerIdentity, Long> {
  Optional<CustomerIdentity> findByCustomerId(String customerId);

  boolean existsByCustomerId(String customerId);
}
