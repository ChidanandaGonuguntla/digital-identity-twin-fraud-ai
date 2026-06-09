package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.api.dto.CustomerVelocityResponse;
import com.citizens.digital.twin.domain.model.Decision;
import com.citizens.digital.twin.domain.model.IdentityTwin;
import com.citizens.digital.twin.domain.model.TransactionEvent;
import com.citizens.digital.twin.infrastructure.persistence.entity.CustomerVelocityFeaturesEntity;
import com.citizens.digital.twin.infrastructure.persistence.repository.CustomerVelocityFeaturesJpaRepository;
import com.citizens.digital.twin.infrastructure.persistence.repository.FraudDecisionAuditJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerVelocityService {
  private final CustomerVelocityFeaturesJpaRepository repository;
  private final FraudDecisionAuditJpaRepository auditRepository;
  private final FeatureStoreService featureStoreService;
  private final Optional<RedisVelocityStore> redisVelocityStore;

  public CustomerVelocityService(
      CustomerVelocityFeaturesJpaRepository repository,
      FraudDecisionAuditJpaRepository auditRepository,
      FeatureStoreService featureStoreService,
      ObjectProvider<RedisVelocityStore> redisVelocityStore) {
    this.repository = repository;
    this.auditRepository = auditRepository;
    this.featureStoreService = featureStoreService;
    this.redisVelocityStore = Optional.ofNullable(redisVelocityStore.getIfAvailable());
  }

  @Transactional
  public void recordTransaction(IdentityTwin twin, TransactionEvent event, Decision decision) {
    redisVelocityStore.ifPresent(
        store -> store.record(event.customerId(), event.amount(), decision));
    CustomerVelocityFeaturesEntity entity =
        repository.findById(event.customerId()).orElseGet(CustomerVelocityFeaturesEntity::new);
    if (entity.getCustomerId() == null) {
      entity.setCustomerId(event.customerId());
    }
    if (redisVelocityStore.isPresent()) {
      syncFromRedis(entity, event.customerId());
    } else {
      recomputeFromAudit(entity);
    }
    if (decision == Decision.BLOCK || decision == Decision.CHALLENGE) {
      if (redisVelocityStore.isEmpty()) {
        entity.setFailedAttempts30m(entity.getFailedAttempts30m() + 1);
      }
    }
    repository.save(entity);
    featureStoreService.upsertCustomerVelocity(entity);
  }

  public CustomerVelocityResponse get(String customerId) {
    Optional<RedisVelocityStore.RedisVelocitySnapshot> redisSnapshot =
        redisVelocityStore.flatMap(store -> store.snapshot(customerId));
    if (redisSnapshot.isPresent()) {
      RedisVelocityStore.RedisVelocitySnapshot snapshot = redisSnapshot.get();
      return new CustomerVelocityResponse(
          customerId,
          snapshot.txnCount5m(),
          snapshot.txnCount1h(),
          snapshot.txnCount24h(),
          snapshot.amountSum1h(),
          0,
          0,
          0,
          snapshot.failedAttempts30m(),
          Instant.now());
    }
    return repository
        .findById(customerId)
        .map(this::toResponse)
        .orElse(new CustomerVelocityResponse(customerId, 0, 0, 0, 0.0, 0, 0, 0, 0, Instant.now()));
  }

  private void syncFromRedis(CustomerVelocityFeaturesEntity entity, String customerId) {
    RedisVelocityStore.RedisVelocitySnapshot snapshot =
        redisVelocityStore.flatMap(store -> store.snapshot(customerId)).orElse(null);
    if (snapshot == null) {
      recomputeFromAudit(entity);
      return;
    }
    entity.setTxnCount5m(snapshot.txnCount5m());
    entity.setTxnCount1h(snapshot.txnCount1h());
    entity.setTxnCount24h(snapshot.txnCount24h());
    entity.setAmountSum1h(BigDecimal.valueOf(snapshot.amountSum1h()));
    entity.setFailedAttempts30m(snapshot.failedAttempts30m());
    entity.setUpdatedAt(Instant.now());
  }

  private void recomputeFromAudit(CustomerVelocityFeaturesEntity entity) {
    Instant now = Instant.now();
    entity.setTxnCount5m(
        (int)
            auditRepository.countByCustomerIdAndAssessedAtAfter(
                entity.getCustomerId(), now.minus(5, ChronoUnit.MINUTES)));
    entity.setTxnCount1h(
        (int)
            auditRepository.countByCustomerIdAndAssessedAtAfter(
                entity.getCustomerId(), now.minus(1, ChronoUnit.HOURS)));
    entity.setTxnCount24h(
        (int)
            auditRepository.countByCustomerIdAndAssessedAtAfter(
                entity.getCustomerId(), now.minus(24, ChronoUnit.HOURS)));
    entity.setAmountSum1h(
        BigDecimal.valueOf(
            auditRepository.sumAmountByCustomerIdSince(
                entity.getCustomerId(), now.minus(1, ChronoUnit.HOURS))));
    entity.setFailedAttempts30m(
        (int)
            auditRepository.countBlockedOrChallengedSince(
                entity.getCustomerId(), now.minus(30, ChronoUnit.MINUTES)));
    entity.setUpdatedAt(now);
  }

  private CustomerVelocityResponse toResponse(CustomerVelocityFeaturesEntity entity) {
    return new CustomerVelocityResponse(
        entity.getCustomerId(),
        entity.getTxnCount5m(),
        entity.getTxnCount1h(),
        entity.getTxnCount24h(),
        entity.getAmountSum1h() == null ? 0.0 : entity.getAmountSum1h().doubleValue(),
        entity.getNewDevices24h(),
        entity.getCountries24h(),
        entity.getCategoryChanges10m(),
        entity.getFailedAttempts30m(),
        entity.getUpdatedAt());
  }
}
