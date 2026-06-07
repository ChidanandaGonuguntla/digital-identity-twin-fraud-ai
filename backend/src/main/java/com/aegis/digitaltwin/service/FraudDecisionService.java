package com.aegis.digitaltwin.service;

import com.aegis.digitaltwin.dto.ActivityEventRequest;
import com.aegis.digitaltwin.dto.FraudDecisionResponse;
import com.aegis.digitaltwin.entity.CustomerEvent;
import com.aegis.digitaltwin.entity.CustomerTwin;
import com.aegis.digitaltwin.repository.CustomerEventRepository;
import com.aegis.digitaltwin.repository.CustomerTwinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FraudDecisionService {
    private final CustomerTwinRepository twinRepository;
    private final CustomerEventRepository eventRepository;
    private final RiskScoringService riskScoringService;
    private final SseEventService sseEventService;

    @Transactional
    public FraudDecisionResponse evaluate(ActivityEventRequest request) {
        CustomerTwin twin = twinRepository.findByCustomerId(request.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer twin not found: " + request.customerId()));
        String eventId = "EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        FraudDecisionResponse response = riskScoringService.evaluate(eventId, twin, request);

        CustomerEvent event = CustomerEvent.builder()
                .eventId(eventId)
                .customerId(request.customerId())
                .eventType(request.eventType())
                .amount(request.amount())
                .merchant(request.merchant())
                .payee(request.payee())
                .deviceId(request.deviceId())
                .location(request.location())
                .ipAddress(request.ipAddress())
                .loginHour(request.loginHour())
                .merchantCategory(request.merchantCategory())
                .riskScore(response.riskScore())
                .decision(response.decision())
                .reasons(response.reasons())
                .build();
        eventRepository.save(event);
        sseEventService.publishDecision(response);
        return response;
    }
}
