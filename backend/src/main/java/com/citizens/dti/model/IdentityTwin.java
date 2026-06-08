package com.citizens.dti.model;

import java.time.Instant;

/**
 * The digital twin of a customer's transactional identity: a virtual model kept synchronized with
 * the real customer's behavior, against which live events are reconciled for fraud detection.
 */
public class IdentityTwin {

  private final String customerId;
  private final BehavioralProfile profile;
  private final Instant createdAt;
  private Instant lastUpdated;

  public IdentityTwin(String customerId) {
    this.customerId = customerId;
    this.profile = new BehavioralProfile();
    this.createdAt = Instant.now();
    this.lastUpdated = this.createdAt;
  }

  /** Reconstruct a persisted twin, preserving its original timestamps. */
  public IdentityTwin(
      String customerId, BehavioralProfile profile, Instant createdAt, Instant lastUpdated) {
    this.customerId = customerId;
    this.profile = profile;
    this.createdAt = createdAt;
    this.lastUpdated = lastUpdated;
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

  public Instant getLastUpdated() {
    return lastUpdated;
  }

  public void markUpdated() {
    this.lastUpdated = Instant.now();
  }
}
