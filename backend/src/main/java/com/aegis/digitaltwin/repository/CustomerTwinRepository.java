package com.aegis.digitaltwin.repository;

import com.aegis.digitaltwin.entity.CustomerTwin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerTwinRepository extends JpaRepository<CustomerTwin, Long> {
    Optional<CustomerTwin> findByCustomerId(String customerId);
    boolean existsByCustomerId(String customerId);
}
