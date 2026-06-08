package com.citizens.dti.api;

import com.citizens.dti.model.RiskAssessment;
import com.citizens.dti.model.TransactionEvent;
import com.citizens.dti.service.FraudDetectionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
public class FraudController {

  private final FraudDetectionService fraudDetectionService;

  public FraudController(FraudDetectionService fraudDetectionService) {
    this.fraudDetectionService = fraudDetectionService;
  }

  /** Score a single live transaction against the customer's identity twin. */
  @PostMapping("/assess")
  public RiskAssessment assess(@Valid @RequestBody TransactionEvent event) {
    return fraudDetectionService.assess(event);
  }

  /** Bootstrap a twin with known-good historical transactions (no scoring). */
  @PostMapping("/seed")
  public ResponseEntity<String> seed(@Valid @RequestBody List<TransactionEvent> events) {
    events.forEach(fraudDetectionService::seed);
    return ResponseEntity.ok("Seeded %d transactions".formatted(events.size()));
  }
}
