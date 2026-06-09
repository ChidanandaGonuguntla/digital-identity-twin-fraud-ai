package com.citizens.digital.twin.api.controller;

import com.citizens.digital.twin.api.dto.PlatformOpsSummaryResponse;
import com.citizens.digital.twin.application.service.PlatformOpsService;
import com.citizens.digital.twin.infrastructure.security.SecurityRoles;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ops")
@RequiredArgsConstructor
public class PlatformOpsController {
  private final PlatformOpsService platformOpsService;

  @GetMapping("/summary")
  @PreAuthorize(
      "@securityExpressions.permitIfDisabled() or hasAnyRole('"
          + SecurityRoles.MODEL_RISK_ADMIN
          + "','"
          + SecurityRoles.ADMIN
          + "')")
  public PlatformOpsSummaryResponse summary() {
    return platformOpsService.summary();
  }
}
