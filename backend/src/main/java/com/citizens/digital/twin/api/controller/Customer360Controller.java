package com.citizens.digital.twin.api.controller;

import com.citizens.digital.twin.api.dto.Customer360TimelineResponse;
import com.citizens.digital.twin.api.dto.CustomerVelocityResponse;
import com.citizens.digital.twin.api.dto.DecisionNarrativeResponse;
import com.citizens.digital.twin.api.dto.FeatureStoreCatalogResponse;
import com.citizens.digital.twin.api.dto.FeatureStoreEntryResponse;
import com.citizens.digital.twin.application.service.Customer360TimelineService;
import com.citizens.digital.twin.application.service.CustomerVelocityService;
import com.citizens.digital.twin.application.service.DecisionNarrativeService;
import com.citizens.digital.twin.application.service.FeatureStoreService;
import com.citizens.digital.twin.infrastructure.security.SecurityRoles;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class Customer360Controller {
  private final Customer360TimelineService timelineService;
  private final CustomerVelocityService velocityService;
  private final FeatureStoreService featureStoreService;
  private final DecisionNarrativeService narrativeService;

  @GetMapping("/customers/{customerId}/timeline")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public Customer360TimelineResponse timeline(@PathVariable String customerId) {
    return timelineService.timeline(customerId);
  }

  @GetMapping("/customers/{customerId}/velocity")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public CustomerVelocityResponse velocity(@PathVariable String customerId) {
    return velocityService.get(customerId);
  }

  @GetMapping("/feature-store")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public List<FeatureStoreEntryResponse> featureStore(
      @RequestParam(required = false) String featureName,
      @RequestParam(required = false) String customerId) {
    if (customerId != null && !customerId.isBlank()) {
      return featureStoreService.forCustomer(customerId.trim().toUpperCase());
    }
    if (featureName != null && !featureName.isBlank()) {
      return featureStoreService.byFeatureName(featureName);
    }
    return featureStoreService.byFeatureName("customer_24h_amount_sum");
  }

  @GetMapping("/feature-store/catalog")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public FeatureStoreCatalogResponse featureStoreCatalog() {
    return featureStoreService.catalog();
  }

  @GetMapping("/decisions/{assessmentId}/narrative")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "','"
          + SecurityRoles.AUDITOR
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public DecisionNarrativeResponse narrative(@PathVariable String assessmentId) {
    return narrativeService.build(assessmentId);
  }
}
