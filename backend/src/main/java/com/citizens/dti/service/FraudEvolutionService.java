package com.citizens.dti.service;

import com.citizens.dti.dto.FraudEvaluationRequest;
import com.citizens.dti.dto.FraudEvaluationResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

@Service
public class FraudEvolutionService {
  public FraudEvaluationResponse evaluate(@Valid FraudEvaluationRequest request) {
    return null;
  }
}
