package com.aegis.digitaltwin.service;

import com.aegis.digitaltwin.domain.DecisionType;
import com.aegis.digitaltwin.dto.ActivityEventRequest;
import com.aegis.digitaltwin.dto.FraudDecisionResponse;
import com.aegis.digitaltwin.entity.CustomerTwin;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class RiskScoringService {
  private static final Set<String> HIGH_RISK_CATEGORIES =
      Set.of("CRYPTO", "GAMBLING", "WIRE", "CASH_OUT");
  private static final Set<String> HIGH_RISK_IP_PREFIXES = Set.of("102.", "185.", "45.");

  public FraudDecisionResponse evaluate(
      String eventId, CustomerTwin twin, ActivityEventRequest request) {
    int score = 0;
    List<String> reasons = new ArrayList<>();
    Map<String, Integer> signalScores = new LinkedHashMap<>();

    if (!containsIgnoreCase(twin.getKnownDevices(), request.deviceId())) {
      score += add(signalScores, "device", 150);
      reasons.add("New device detected: " + request.deviceId());
    } else {
      signalScores.put("device", 0);
    }

    if (!containsIgnoreCase(twin.getKnownLocations(), request.location())) {
      score += add(signalScores, "location", 150);
      reasons.add("Unusual location detected: " + request.location());
    } else {
      signalScores.put("location", 0);
    }

    if (request.amount() != null && twin.getAverageTransactionAmount() != null) {
      BigDecimal threshold = twin.getAverageTransactionAmount().multiply(BigDecimal.valueOf(5));
      if (request.amount().compareTo(threshold) > 0) {
        score += add(signalScores, "amount", 200);
        reasons.add("Amount is more than 5x the normal transaction baseline");
      } else {
        signalScores.put("amount", 0);
      }
    }

    if (request.merchant() != null
        && !containsIgnoreCase(twin.getTrustedMerchants(), request.merchant())) {
      score += add(signalScores, "merchant", 100);
      reasons.add("Unknown merchant detected: " + request.merchant());
    } else {
      signalScores.put("merchant", 0);
    }

    if (request.payee() != null
        && !request.payee().isBlank()
        && !containsIgnoreCase(twin.getTrustedPayees(), request.payee())) {
      score += add(signalScores, "payee", 125);
      reasons.add("New or untrusted payee detected");
    } else {
      signalScores.put("payee", 0);
    }

    if (request.loginHour() != null && !isNormalHour(twin, request.loginHour())) {
      score += add(signalScores, "loginHour", 75);
      reasons.add("Activity occurred outside normal login hours");
    } else {
      signalScores.put("loginHour", 0);
    }

    if (request.merchantCategory() != null
        && HIGH_RISK_CATEGORIES.contains(request.merchantCategory().toUpperCase())) {
      score += add(signalScores, "merchantCategory", 250);
      reasons.add("High-risk merchant category: " + request.merchantCategory());
    } else {
      signalScores.put("merchantCategory", 0);
    }

    if (request.ipAddress() != null
        && HIGH_RISK_IP_PREFIXES.stream().anyMatch(request.ipAddress()::startsWith)) {
      score += add(signalScores, "ipReputation", 175);
      reasons.add("IP address belongs to a high-risk reputation band");
    } else {
      signalScores.put("ipReputation", 0);
    }

    if (twin.getRecentFailedAuthCount() != null && twin.getRecentFailedAuthCount() >= 3) {
      score += add(signalScores, "recentFailedAuth", 150);
      reasons.add("Recent failed authentication attempts increased risk");
    } else {
      signalScores.put("recentFailedAuth", 0);
    }

    DecisionType decision = decide(score);
    if (reasons.isEmpty()) {
      reasons.add("Activity is consistent with the customer digital identity twin");
    }
    return new FraudDecisionResponse(
        eventId, request.customerId(), score, decision, reasons, signalScores, Instant.now());
  }

  private int add(Map<String, Integer> signalScores, String key, int points) {
    signalScores.put(key, points);
    return points;
  }

  private boolean containsIgnoreCase(List<String> values, String input) {
    if (values == null || input == null) return false;
    return values.stream().anyMatch(v -> v.equalsIgnoreCase(input));
  }

  private boolean isNormalHour(CustomerTwin twin, int hour) {
    int start = twin.getNormalLoginStartHour() == null ? 6 : twin.getNormalLoginStartHour();
    int end = twin.getNormalLoginEndHour() == null ? 22 : twin.getNormalLoginEndHour();
    return hour >= start && hour <= end;
  }

  private DecisionType decide(int score) {
    if (score >= 700) return DecisionType.BLOCK;
    if (score >= 400) return DecisionType.MANUAL_REVIEW;
    if (score >= 200) return DecisionType.STEP_UP_AUTH;
    return DecisionType.ALLOW;
  }
}
