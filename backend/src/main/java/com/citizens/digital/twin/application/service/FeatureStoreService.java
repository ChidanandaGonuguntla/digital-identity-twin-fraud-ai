package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.api.dto.FeatureStoreCatalogResponse;
import com.citizens.digital.twin.api.dto.FeatureStoreEntryResponse;
import com.citizens.digital.twin.infrastructure.persistence.entity.CustomerVelocityFeaturesEntity;
import com.citizens.digital.twin.infrastructure.persistence.entity.FeatureStoreValueEntity;
import com.citizens.digital.twin.infrastructure.persistence.repository.FeatureStoreValueJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeatureStoreService {
  private final FeatureStoreValueJpaRepository repository;

  public FeatureStoreService(FeatureStoreValueJpaRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public void upsertCustomerVelocity(CustomerVelocityFeaturesEntity velocity) {
    upsert(
        "customer:" + velocity.getCustomerId(),
        "customer_7d_transaction_count",
        velocity.getTxnCount24h());
    upsert(
        "customer:" + velocity.getCustomerId(),
        "customer_24h_amount_sum",
        velocity.getAmountSum1h());
    upsert(
        "customer:" + velocity.getCustomerId(),
        "customer_5m_transaction_count",
        velocity.getTxnCount5m());
    upsert(
        "customer:" + velocity.getCustomerId(),
        "customer_1h_transaction_count",
        velocity.getTxnCount1h());
    upsert(
        "customer:" + velocity.getCustomerId(),
        "customer_failed_attempts_30m",
        velocity.getFailedAttempts30m());
  }

  @Transactional
  public void upsert(String entityPrefix, String featureName, BigDecimal value) {
    upsert(entityPrefix, featureName, value == null ? 0.0 : value.doubleValue());
  }

  @Transactional
  public void upsert(String entityPrefix, String featureName, int value) {
    upsert(entityPrefix, featureName, (double) value);
  }

  @Transactional
  public void upsert(String entityPrefix, String featureName, double value) {
    String key = entityPrefix + ":" + featureName;
    FeatureStoreValueEntity entity =
        repository.findById(key).orElseGet(FeatureStoreValueEntity::new);
    entity.setEntityKey(key);
    entity.setFeatureName(featureName);
    entity.setFeatureValue(BigDecimal.valueOf(value));
    entity.setUpdatedAt(Instant.now());
    repository.save(entity);
  }

  public List<FeatureStoreEntryResponse> byFeatureName(String featureName) {
    return repository.findByFeatureNameOrderByUpdatedAtDesc(featureName).stream()
        .limit(100)
        .map(
            e ->
                new FeatureStoreEntryResponse(
                    e.getEntityKey(),
                    e.getFeatureName(),
                    e.getFeatureValue().doubleValue(),
                    e.getUpdatedAt()))
        .toList();
  }

  public List<FeatureStoreEntryResponse> forCustomer(String customerId) {
    String prefix = "customer:" + customerId + ":";
    return repository.findAll().stream()
        .filter(e -> e.getEntityKey().startsWith(prefix))
        .map(
            e ->
                new FeatureStoreEntryResponse(
                    e.getEntityKey(),
                    e.getFeatureName(),
                    e.getFeatureValue().doubleValue(),
                    e.getUpdatedAt()))
        .toList();
  }

  public FeatureStoreCatalogResponse catalog() {
    List<String> names = repository.findDistinctFeatureNames();
    if (names.isEmpty()) {
      names =
          List.of(
              "customer_5m_transaction_count",
              "customer_1h_transaction_count",
              "customer_7d_transaction_count",
              "customer_24h_amount_sum",
              "customer_failed_attempts_30m");
    }
    return new FeatureStoreCatalogResponse(names, repository.count());
  }
}
