package com.citizens.digital.twin.domain.rule;

import com.citizens.digital.twin.domain.model.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class RuleRiskEngine {
  private static final Set<String> BLOCKED_COUNTRIES = Set.of("KP", "IR", "SY");
  private static final Set<String> HIGH_RISK_CHANNELS = Set.of("UNKNOWN", "TOR", "PROXY");

  public RuleScore evaluate(IdentityTwin twin, TransactionEvent event) {
    List<RiskSignal> signals = new ArrayList<>();
    if (event.amount() >= 5000)
      signals.add(
          signal(
              "HIGH_VALUE_TRANSACTION",
              "High-value transaction exceeded rule threshold.",
              Severity.HIGH,
              25,
              "amount=" + event.amount()));
    if (event.countryCode() != null
        && BLOCKED_COUNTRIES.contains(event.countryCode().trim().toUpperCase()))
      signals.add(
          signal(
              "BLOCKED_COUNTRY",
              "Transaction originated from blocked country.",
              Severity.CRITICAL,
              100,
              "countryCode=" + event.countryCode()));
    if (event.channel() != null
        && HIGH_RISK_CHANNELS.contains(event.channel().trim().toUpperCase()))
      signals.add(
          signal(
              "HIGH_RISK_CHANNEL",
              "Transaction arrived from high-risk channel.",
              Severity.HIGH,
              20,
              "channel=" + event.channel()));
    BigDecimal score =
        signals.stream()
            .map(RiskSignal::scoreContribution)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .min(BigDecimal.valueOf(100));
    return new RuleScore(score, signals);
  }

  private RiskSignal signal(
      String code, String msg, Severity severity, double contribution, String evidence) {
    return new RiskSignal(
        RiskSignalType.RULE, code, msg, severity, BigDecimal.valueOf(contribution), evidence);
  }

  public record RuleScore(BigDecimal score, List<RiskSignal> signals) {}
}
