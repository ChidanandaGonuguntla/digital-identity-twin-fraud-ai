package com.citizens.digital.twin.api.dto;

import com.citizens.digital.twin.domain.model.RiskAssessment;
import com.citizens.digital.twin.domain.model.RiskSignal;
import com.citizens.digital.twin.domain.model.TransactionEvent;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DecisionEventFactory {
  public DecisionEvent from(TransactionEvent e, RiskAssessment a) {
    return new DecisionEvent(
        a.assessmentId(),
        a.transactionId(),
        a.customerId(),
        e.amount(),
        e.merchantCategory(),
        e.deviceId(),
        e.latitude(),
        e.longitude(),
        e.timestamp().toEpochMilli(),
        a.riskScore(),
        a.decision().name(),
        isColdStart(a),
        deriveSignals(a),
        reasonMessages(a));
  }

  private boolean isColdStart(RiskAssessment a) {
    return a.reasonCodes() != null
        && a.reasonCodes().stream().anyMatch(s -> "TWIN_COLD_START".equalsIgnoreCase(s.code()));
  }

  private List<SignalContribution> deriveSignals(RiskAssessment a) {
    if (a.reasonCodes() == null) return List.of();
    return a.reasonCodes().stream()
        .filter(s -> s.scoreContribution() != null && s.scoreContribution().doubleValue() > 0)
        .map(s -> new SignalContribution(toSignalName(s), s.scoreContribution().doubleValue()))
        .sorted(Comparator.comparingDouble(SignalContribution::contribution).reversed())
        .toList();
  }

  private List<String> reasonMessages(RiskAssessment a) {
    if (a.reasonCodes() == null) return List.of();
    return a.reasonCodes().stream().map(this::formatReason).toList();
  }

  private String formatReason(RiskSignal s) {
    return s.evidence() == null || s.evidence().isBlank()
        ? s.message()
        : s.message() + " [" + s.evidence() + "]";
  }

  private String toSignalName(RiskSignal s) {
    return s.code() == null || s.code().isBlank()
        ? s.type().name().toLowerCase()
        : s.code().trim().toLowerCase();
  }
}
