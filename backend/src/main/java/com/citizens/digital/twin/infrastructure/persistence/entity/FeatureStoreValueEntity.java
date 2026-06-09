package com.citizens.digital.twin.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "feature_store_values", schema = "identity_twin")
public class FeatureStoreValueEntity {

  @Id
  @Column(name = "entity_key", nullable = false, length = 160)
  private String entityKey;

  @Column(name = "feature_name", nullable = false, length = 120)
  private String featureName;

  @Column(name = "feature_value", nullable = false, precision = 14, scale = 6)
  private BigDecimal featureValue;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void prePersist() {
    if (updatedAt == null) {
      updatedAt = Instant.now();
    }
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = Instant.now();
  }

  public String getEntityKey() {
    return entityKey;
  }

  public void setEntityKey(String entityKey) {
    this.entityKey = entityKey;
  }

  public String getFeatureName() {
    return featureName;
  }

  public void setFeatureName(String featureName) {
    this.featureName = featureName;
  }

  public BigDecimal getFeatureValue() {
    return featureValue;
  }

  public void setFeatureValue(BigDecimal featureValue) {
    this.featureValue = featureValue;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
