package com.citizens.dti.persistence;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TwinBaseline {

  private Set<String> knownDevices = new HashSet<>();
  private Map<String, Integer> categoryCounts = new HashMap<>();
  private long[] hourHistogram = new long[24];
  private Double lastLatitude;
  private Double lastLongitude;
  private Long lastTimestampEpochSeconds;
  private String lastMerchantCategory;
}
