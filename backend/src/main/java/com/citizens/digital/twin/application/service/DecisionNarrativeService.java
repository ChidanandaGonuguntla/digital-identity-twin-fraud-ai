package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.api.dto.DecisionNarrativeResponse;
import org.springframework.stereotype.Service;

@Service
public class DecisionNarrativeService {
  private final DecisionExplainabilityService explainabilityService;

  public DecisionNarrativeService(DecisionExplainabilityService explainabilityService) {
    this.explainabilityService = explainabilityService;
  }

  public DecisionNarrativeResponse build(String assessmentId) {
    return explainabilityService.build(assessmentId);
  }
}
