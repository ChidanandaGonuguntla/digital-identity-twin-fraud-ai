package com.aegis.digitaltwin.service;

import com.aegis.digitaltwin.domain.DecisionType;
import com.aegis.digitaltwin.dto.DashboardResponse;
import com.aegis.digitaltwin.repository.CustomerEventRepository;
import com.aegis.digitaltwin.repository.CustomerTwinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final CustomerTwinRepository twinRepository;
    private final CustomerEventRepository eventRepository;

    public DashboardResponse getDashboard() {
        long totalEvents = eventRepository.count();
        long allowed = eventRepository.countByDecision(DecisionType.ALLOW);
        long stepUp = eventRepository.countByDecision(DecisionType.STEP_UP_AUTH);
        long review = eventRepository.countByDecision(DecisionType.MANUAL_REVIEW);
        long blocked = eventRepository.countByDecision(DecisionType.BLOCK);
        double pressure = totalEvents == 0 ? 0 : ((blocked * 1.0 + review * 0.65 + stepUp * 0.35) / totalEvents) * 100;
        return new DashboardResponse(twinRepository.count(), totalEvents, allowed, stepUp, review, blocked, Math.round(pressure * 10.0) / 10.0);
    }
}
