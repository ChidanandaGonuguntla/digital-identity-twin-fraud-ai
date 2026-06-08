package com.aegis.digitaltwin.controller;

import com.aegis.digitaltwin.dto.ActivityEventRequest;
import com.aegis.digitaltwin.dto.FeedbackRequest;
import com.aegis.digitaltwin.dto.FraudDecisionResponse;
import com.aegis.digitaltwin.entity.CustomerEvent;
import com.aegis.digitaltwin.entity.CustomerTwin;
import com.aegis.digitaltwin.repository.CustomerEventRepository;
import com.aegis.digitaltwin.service.FeedbackLearningService;
import com.aegis.digitaltwin.service.FraudDecisionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fraud")
@RequiredArgsConstructor
public class FraudController {
  private final FraudDecisionService fraudDecisionService;
  private final FeedbackLearningService feedbackLearningService;
  private final CustomerEventRepository eventRepository;

  @PostMapping("/evaluate")
  public FraudDecisionResponse evaluate(@Valid @RequestBody ActivityEventRequest request) {
    return fraudDecisionService.evaluate(request);
  }

  @PostMapping("/feedback")
  public CustomerTwin feedback(@Valid @RequestBody FeedbackRequest request) {
    return feedbackLearningService.applyFeedback(request);
  }

  @GetMapping("/events/recent")
  public List<CustomerEvent> recentEvents() {
    return eventRepository.findTop20ByOrderByCreatedAtDesc();
  }
}
