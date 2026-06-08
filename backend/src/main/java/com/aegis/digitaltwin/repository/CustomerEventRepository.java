package com.aegis.digitaltwin.repository;

import com.aegis.digitaltwin.domain.DecisionType;
import com.aegis.digitaltwin.entity.CustomerEvent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerEventRepository extends JpaRepository<CustomerEvent, Long> {

  Optional<CustomerEvent> findByEventId(String eventId);

  List<CustomerEvent> findTop20ByOrderByCreatedAtDesc();

  List<CustomerEvent> findTop10ByCustomerIdOrderByCreatedAtDesc(String customerId);

  long countByDecision(DecisionType decision);
}
