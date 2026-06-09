package com.citizens.digital.twin.infrastructure.persistence.repository;

import com.citizens.digital.twin.infrastructure.persistence.entity.FraudCaseEntity;
import org.springframework.data.jpa.domain.Specification;

public final class FraudCaseSpecifications {
  private FraudCaseSpecifications() {}

  public static Specification<FraudCaseEntity> withFilters(
      String status, String customerId, String transactionId, String assignedTo, String priority) {
    return Specification.where(statusEquals(status))
        .and(customerIdEquals(customerId))
        .and(transactionIdEquals(transactionId))
        .and(assignedToEquals(assignedTo))
        .and(priorityEquals(priority));
  }

  private static Specification<FraudCaseEntity> statusEquals(String status) {
    if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
      return null;
    }
    return (root, query, builder) ->
        builder.equal(builder.upper(root.get("status")), status.trim().toUpperCase());
  }

  private static Specification<FraudCaseEntity> customerIdEquals(String customerId) {
    if (customerId == null || customerId.isBlank()) {
      return null;
    }
    return (root, query, builder) ->
        builder.like(
            builder.upper(root.get("customerId")), "%" + customerId.trim().toUpperCase() + "%");
  }

  private static Specification<FraudCaseEntity> transactionIdEquals(String transactionId) {
    if (transactionId == null || transactionId.isBlank()) {
      return null;
    }
    return (root, query, builder) ->
        builder.like(
            builder.upper(root.get("transactionId")),
            "%" + transactionId.trim().toUpperCase() + "%");
  }

  private static Specification<FraudCaseEntity> assignedToEquals(String assignedTo) {
    if (assignedTo == null || assignedTo.isBlank()) {
      return null;
    }
    return (root, query, builder) ->
        builder.like(
            builder.upper(root.get("assignedTo")), "%" + assignedTo.trim().toUpperCase() + "%");
  }

  private static Specification<FraudCaseEntity> priorityEquals(String priority) {
    if (priority == null || priority.isBlank() || "ALL".equalsIgnoreCase(priority)) {
      return null;
    }
    return (root, query, builder) ->
        builder.equal(builder.upper(root.get("priority")), priority.trim().toUpperCase());
  }
}
