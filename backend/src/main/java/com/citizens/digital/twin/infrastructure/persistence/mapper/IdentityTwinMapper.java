package com.citizens.digital.twin.infrastructure.persistence.mapper;

import com.citizens.digital.twin.domain.model.BehavioralProfile;
import com.citizens.digital.twin.domain.model.IdentityTwin;
import com.citizens.digital.twin.infrastructure.persistence.entity.IdentityTwinEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class IdentityTwinMapper {
  private final ObjectMapper objectMapper;

  public IdentityTwinMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public IdentityTwin toDomain(IdentityTwinEntity e) {
    BehavioralProfile p =
        BehavioralProfile.restore(
            e.getTransactionCount(),
            e.getAmountSum(),
            e.getAmountSumOfSquares(),
            csvToSet(e.getKnownDevicesCsv()),
            csvToSet(e.getUsualCountriesCsv()),
            readMap(e.getMerchantCategoryCountsJson()),
            csvToLongArray(e.getHourHistogramCsv()),
            e.getLastLatitude(),
            e.getLastLongitude(),
            e.getLastTimestampEpochSeconds(),
            e.getLastMerchantCategory());
    return new IdentityTwin(e.getCustomerId(), p, e.getCreatedAt(), e.getUpdatedAt());
  }

  public IdentityTwinEntity toEntity(IdentityTwin twin) {
    IdentityTwinEntity e = new IdentityTwinEntity();
    applyToEntity(e, twin);
    return e;
  }

  public void applyToEntity(IdentityTwinEntity e, IdentityTwin twin) {
    BehavioralProfile p = twin.getProfile();
    e.setCustomerId(twin.getCustomerId());
    e.setTransactionCount(p.getTransactionCount());
    e.setAmountSum(p.getAmountSum());
    e.setAmountSumOfSquares(p.getAmountSumOfSquares());
    e.setKnownDevicesCsv(setToCsv(p.getKnownDevices()));
    e.setUsualCountriesCsv(setToCsv(p.getUsualCountries()));
    e.setMerchantCategoryCountsJson(write(p.getMerchantCategoryCounts()));
    e.setHourHistogramCsv(longArrayToCsv(p.getHourHistogram()));
    e.setLastLatitude(p.getLastLatitude());
    e.setLastLongitude(p.getLastLongitude());
    e.setLastTimestampEpochSeconds(p.getLastTimestampEpochSeconds());
    e.setLastMerchantCategory(p.getLastMerchantCategory());
    e.setCreatedAt(twin.getCreatedAt());
    e.setUpdatedAt(twin.getUpdatedAt());
  }

  private Set<String> csvToSet(String csv) {
    if (csv == null || csv.isBlank()) return Set.of();
    return Arrays.stream(csv.split(",")).filter(s -> !s.isBlank()).collect(Collectors.toSet());
  }

  private String setToCsv(Set<String> set) {
    return set == null ? "" : String.join(",", set);
  }

  private long[] csvToLongArray(String csv) {
    long[] out = new long[24];
    if (csv == null || csv.isBlank()) return out;
    String[] parts = csv.split(",");
    for (int i = 0; i < Math.min(24, parts.length); i++) {
      try {
        out[i] = Long.parseLong(parts[i]);
      } catch (Exception ignored) {
      }
    }
    return out;
  }

  private String longArrayToCsv(long[] arr) {
    return Arrays.stream(arr).mapToObj(String::valueOf).collect(Collectors.joining(","));
  }

  private Map<String, Integer> readMap(String json) {
    try {
      return json == null || json.isBlank()
          ? Map.of()
          : objectMapper.readValue(json, new TypeReference<Map<String, Integer>>() {});
    } catch (Exception e) {
      return Map.of();
    }
  }

  private String write(Object v) {
    try {
      return objectMapper.writeValueAsString(v);
    } catch (Exception e) {
      return "{}";
    }
  }
}
