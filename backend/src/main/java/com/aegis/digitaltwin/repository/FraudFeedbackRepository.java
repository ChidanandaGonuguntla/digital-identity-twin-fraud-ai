package com.aegis.digitaltwin.repository;

import com.aegis.digitaltwin.entity.FraudFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudFeedbackRepository extends JpaRepository<FraudFeedback, Long> {}
