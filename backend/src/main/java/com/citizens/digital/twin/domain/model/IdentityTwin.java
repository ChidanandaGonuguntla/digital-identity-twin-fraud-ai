package com.citizens.digital.twin.domain.model;

import java.time.Instant;
import java.util.Set;

public class IdentityTwin {
  private final String customerId;
  private final BehavioralProfile profile;
  private final Instant createdAt;
  private Instant updatedAt;

  public IdentityTwin(
      String customerId, BehavioralProfile profile, Instant createdAt, Instant updatedAt) {
    this.customerId = customerId;
    this.profile = profile == null ? new BehavioralProfile() : profile;
    this.createdAt = createdAt == null ? Instant.now() : createdAt;
    this.updatedAt = updatedAt == null ? Instant.now() : updatedAt;
  }

  public static IdentityTwin newTwin(String customerId) {
    return new IdentityTwin(customerId, new BehavioralProfile(), Instant.now(), Instant.now());
  }

  public String getCustomerId() {
    return customerId;
  }

  public BehavioralProfile getProfile() {
    return profile;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void markUpdated() {
    this.updatedAt = Instant.now();
  }

  public Set<String> usualCountries() {
    return profile.getUsualCountries();
  }

  public boolean isUsualCountry(String countryCode) {
    return profile.isUsualCountry(countryCode);
  }
}
