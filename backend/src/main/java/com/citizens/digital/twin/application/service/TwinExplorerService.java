package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.api.dto.MerchantCategoryMemoryItem;
import com.citizens.digital.twin.api.dto.TwinExplorerResponse;
import com.citizens.digital.twin.domain.model.BehavioralProfile;
import com.citizens.digital.twin.domain.model.IdentityTwin;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TwinExplorerService {
  private static final Set<String> HIGH_RISK_CATEGORIES =
      Set.of(
          "jewellery",
          "jewelry",
          "electronics",
          "crypto",
          "gift_card",
          "gift card",
          "travel",
          "gambling");

  private final IdentityTwinService identityTwinService;
  private final int minHistory;

  public TwinExplorerService(
      IdentityTwinService identityTwinService,
      @Value("${dti.scoring.min-history:3}") int minHistory) {
    this.identityTwinService = identityTwinService;
    this.minHistory = minHistory;
  }

  public TwinExplorerResponse explore(String customerId) {
    String resolvedId = resolveCustomerId(customerId);
    IdentityTwin twin = identityTwinService.getOrCreateTwin(resolvedId);
    BehavioralProfile profile = twin.getProfile();
    long txnCount = profile.getTransactionCount();
    List<MerchantCategoryMemoryItem> categories =
        profile.getMerchantCategoryCounts().entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
            .limit(8)
            .map(
                e ->
                    new MerchantCategoryMemoryItem(
                        e.getKey(),
                        formatCategoryLabel(e.getKey()),
                        e.getValue(),
                        round2(txnCount == 0 ? 0.0 : e.getValue() / (double) txnCount),
                        round1(riskOverlay(e.getKey(), e.getValue(), txnCount))))
            .toList();
    String lastMerchantCategory = formatCategoryLabel(profile.getLastMerchantCategory());
    String topMerchantCategory = categories.isEmpty() ? "" : categories.get(0).displayCategory();

    return new TwinExplorerResponse(
        twin.getCustomerId(),
        txnCount,
        round2(profile.amountMean()),
        round2(profile.amountStdDev()),
        round2(profile.getAmountSum()),
        lastMerchantCategory,
        topMerchantCategory,
        profile.getKnownDevices().stream().sorted().toList(),
        profile.getUsualCountries().stream().sorted().toList(),
        categories,
        twin.getCreatedAt(),
        twin.getUpdatedAt(),
        txnCount < minHistory);
  }

  private double riskOverlay(String category, int count, long total) {
    if (total == 0) return 0.0;
    double frequency = count / (double) total;
    double risk = 0.0;
    if (HIGH_RISK_CATEGORIES.contains(normalize(category))) risk += 42.0;
    if (frequency < 0.05) risk += 28.0;
    if (count <= 1) risk += 20.0;
    return Math.min(100.0, risk);
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase();
  }

  private String formatCategoryLabel(String category) {
    if (category == null || category.isBlank()) {
      return "";
    }
    String[] parts = category.trim().toLowerCase().replace('_', ' ').split("\\s+");
    StringBuilder builder = new StringBuilder();
    for (String part : parts) {
      if (part.isBlank()) {
        continue;
      }
      if (!builder.isEmpty()) {
        builder.append(' ');
      }
      builder.append(Character.toUpperCase(part.charAt(0)));
      if (part.length() > 1) {
        builder.append(part.substring(1));
      }
    }
    return builder.toString();
  }

  private String resolveCustomerId(String customerId) {
    if (customerId == null || customerId.isBlank()) {
      return customerId;
    }
    return customerId.trim().toUpperCase();
  }

  private double round1(double value) {
    return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
  }

  private double round2(double value) {
    return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
  }
}
