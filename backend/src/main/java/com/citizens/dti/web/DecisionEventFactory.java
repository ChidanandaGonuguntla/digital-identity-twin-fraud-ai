package com.citizens.dti.web;

import com.citizens.dti.model.RiskAssessment;
import com.citizens.dti.model.TransactionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Builds the {@link DecisionEvent} wire object from the domain pair
 * (TransactionEvent + RiskAssessment).
 *
 * <p>The deterministic engine emits human-readable reason codes but not per-signal
 * numeric contributions, so we derive structured {@link SignalContribution}s from the
 * reasons and distribute the composite risk score across the detected signals using
 * their default weights. This drives the console's signal-breakdown bars. If you later
 * have the scoring engine emit signals directly, replace {@link #deriveSignals} with a
 * pass-through.
 */
@Component
public class DecisionEventFactory {

    // Reason-text keyword -> (signal name, default weight). Mirrors DeviationScoringEngine.
    private record SignalRule(String keyword, String name, double weight) {}

    private static final List<SignalRule> RULES = List.of(
            new SignalRule("impossible travel", "geo_velocity", 40),
            new SignalRule("simultaneous transactions", "geo_velocity", 40),
            new SignalRule("fast travel", "geo_velocity", 40),
            new SignalRule("\u03c3 above", "amount_anomaly", 30),   // matches "Nσ above"
            new SignalRule("amount", "amount_anomaly", 30),
            new SignalRule("device", "new_device", 20),
            new SignalRule("hour", "unusual_hour", 15),
            new SignalRule("merchant category", "new_category", 15)
    );

    public DecisionEvent from(TransactionEvent e, RiskAssessment a) {
        return new DecisionEvent(
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
                a.coldStart(),
                deriveSignals(a),
                a.reasons()
        );
    }

    private List<SignalContribution> deriveSignals(RiskAssessment a) {
        if (a.reasons() == null || a.reasons().isEmpty()) {
            return List.of();
        }
        // Detect which signals fired, de-duplicated by signal name, preserving first match.
        List<SignalRule> matched = new ArrayList<>();
        for (String reason : a.reasons()) {
            String lower = reason.toLowerCase(Locale.ROOT);
            for (SignalRule rule : RULES) {
                if (lower.contains(rule.keyword())
                        && matched.stream().noneMatch(m -> m.name().equals(rule.name()))) {
                    matched.add(rule);
                }
            }
        }
        if (matched.isEmpty()) {
            return List.of();
        }
        // Distribute the composite score across detected signals by their default weight.
        double weightSum = matched.stream().mapToDouble(SignalRule::weight).sum();
        double scale = a.riskScore() > 0 ? a.riskScore() / weightSum : 1.0;
        List<SignalContribution> signals = new ArrayList<>(matched.size());
        for (SignalRule rule : matched) {
            double contribution = Math.round(rule.weight() * scale * 10.0) / 10.0;
            signals.add(new SignalContribution(rule.name(), contribution));
        }
        signals.sort((x, y) -> Double.compare(y.contribution(), x.contribution()));
        return signals;
    }
}
