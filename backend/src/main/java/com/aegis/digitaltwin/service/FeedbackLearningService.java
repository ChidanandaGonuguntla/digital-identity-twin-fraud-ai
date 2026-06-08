package com.aegis.digitaltwin.service;

import com.aegis.digitaltwin.domain.FeedbackOutcome;
import com.aegis.digitaltwin.domain.RiskLevel;
import com.aegis.digitaltwin.dto.FeedbackRequest;
import com.aegis.digitaltwin.entity.CustomerEvent;
import com.aegis.digitaltwin.entity.CustomerTwin;
import com.aegis.digitaltwin.entity.FraudFeedback;
import com.aegis.digitaltwin.repository.CustomerEventRepository;
import com.aegis.digitaltwin.repository.CustomerTwinRepository;
import com.aegis.digitaltwin.repository.FraudFeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackLearningService {
  private final FraudFeedbackRepository feedbackRepository;
  private final CustomerEventRepository eventRepository;
  private final CustomerTwinRepository twinRepository;

  @Transactional
  public CustomerTwin applyFeedback(FeedbackRequest request) {
    CustomerEvent event =
        eventRepository
            .findByEventId(request.eventId())
            .orElseThrow(
                () -> new IllegalArgumentException("Event not found: " + request.eventId()));
    CustomerTwin twin =
        twinRepository
            .findByCustomerId(request.customerId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Customer twin not found: " + request.customerId()));

    feedbackRepository.save(
        FraudFeedback.builder()
            .eventId(request.eventId())
            .customerId(request.customerId())
            .outcome(request.outcome())
            .comments(request.comments())
            .build());

    if (request.outcome() == FeedbackOutcome.LEGITIMATE
        || request.outcome() == FeedbackOutcome.FALSE_POSITIVE) {
      if (event.getDeviceId() != null && !twin.getKnownDevices().contains(event.getDeviceId()))
        twin.getKnownDevices().add(event.getDeviceId());
      if (event.getLocation() != null && !twin.getKnownLocations().contains(event.getLocation()))
        twin.getKnownLocations().add(event.getLocation());
      if (event.getMerchant() != null && !twin.getTrustedMerchants().contains(event.getMerchant()))
        twin.getTrustedMerchants().add(event.getMerchant());
      if (event.getPayee() != null && !twin.getTrustedPayees().contains(event.getPayee()))
        twin.getTrustedPayees().add(event.getPayee());
      twin.setTrustScore(Math.min(1000, twin.getTrustScore() + 25));
      twin.setRiskLevel(twin.getTrustScore() > 750 ? RiskLevel.LOW : RiskLevel.MEDIUM);
    }

    if (request.outcome() == FeedbackOutcome.CONFIRMED_FRAUD) {
      twin.setTrustScore(Math.max(100, twin.getTrustScore() - 120));
      twin.setRiskLevel(twin.getTrustScore() < 450 ? RiskLevel.CRITICAL : RiskLevel.HIGH);
      twin.setLastHighRiskEventAt(event.getCreatedAt());
    }

    return twinRepository.save(twin);
  }
}
