package com.citizens.digital.twin.api.controller;

import com.citizens.digital.twin.api.dto.TwinExplorerResponse;
import com.citizens.digital.twin.api.dto.TwinSummaryResponse;
import com.citizens.digital.twin.application.service.IdentityTwinService;
import com.citizens.digital.twin.application.service.TwinExplorerService;
import com.citizens.digital.twin.domain.model.IdentityTwin;
import com.citizens.digital.twin.infrastructure.security.SecurityRoles;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/twins")
@RequiredArgsConstructor
public class TwinController {

  private final IdentityTwinService identityTwinService;
  private final TwinExplorerService twinExplorerService;

  @GetMapping("/{customerId}")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public TwinSummaryResponse get(@PathVariable String customerId) {
    IdentityTwin t = identityTwinService.getOrCreateTwin(customerId);
    return new TwinSummaryResponse(
        t.getCustomerId(),
        t.getProfile().getTransactionCount(),
        t.getProfile().amountMean(),
        t.getProfile().getKnownDevices(),
        t.getProfile().getUsualCountries());
  }

  @GetMapping("/{customerId}/explorer")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.FRAUD_ANALYST
          + "','"
          + SecurityRoles.FRAUD_MANAGER
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public TwinExplorerResponse explorer(@PathVariable String customerId) {
    return twinExplorerService.explore(customerId);
  }
}
