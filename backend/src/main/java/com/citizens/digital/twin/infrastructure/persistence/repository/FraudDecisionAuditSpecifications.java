package com.citizens.digital.twin.infrastructure.persistence.repository;

import com.citizens.digital.twin.infrastructure.persistence.entity.FraudDecisionAuditEntity;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;

public final class FraudDecisionAuditSpecifications {
  private FraudDecisionAuditSpecifications() {}

  public static Specification<FraudDecisionAuditEntity> withFilters(
      String decision,
      String customerId,
      String transactionId,
      Instant from,
      Instant to,
      BigDecimal minScore,
      BigDecimal maxScore) {
    return Specification.where(decisionEquals(decision))
        .and(customerIdEquals(customerId))
        .and(transactionIdEquals(transactionId))
        .and(assessedFrom(from))
        .and(assessedTo(to))
        .and(scoreAtLeast(minScore))
        .and(scoreAtMost(maxScore));
  }

  private static Specification<FraudDecisionAuditEntity> decisionEquals(String decision) {
    return (root, query, cb) ->
        decision == null ? cb.conjunction() : cb.equal(root.get("decision"), decision);
  }

  private static Specification<FraudDecisionAuditEntity> customerIdEquals(String customerId) {
    return (root, query, cb) ->
        customerId == null ? cb.conjunction() : cb.equal(root.get("customerId"), customerId);
  }

  private static Specification<FraudDecisionAuditEntity> transactionIdEquals(String transactionId) {
    return (root, query, cb) ->
        transactionId == null
            ? cb.conjunction()
            : cb.equal(root.get("transactionId"), transactionId);
  }

  private static Specification<FraudDecisionAuditEntity> assessedFrom(Instant from) {
    return (root, query, cb) ->
        from == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("assessedAt"), from);
  }

  private static Specification<FraudDecisionAuditEntity> assessedTo(Instant to) {
    return (root, query, cb) ->
        to == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("assessedAt"), to);
  }

  private static Specification<FraudDecisionAuditEntity> scoreAtLeast(BigDecimal minScore) {
    return (root, query, cb) ->
        minScore == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("finalScore"), minScore);
  }

  private static Specification<FraudDecisionAuditEntity> scoreAtMost(BigDecimal maxScore) {
    return (root, query, cb) ->
        maxScore == null
            ? cb.conjunction()
            : cb.lessThanOrEqualTo(root.get("finalScore"), maxScore);
  }
}
