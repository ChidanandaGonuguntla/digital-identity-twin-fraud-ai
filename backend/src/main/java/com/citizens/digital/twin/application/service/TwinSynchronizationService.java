package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.domain.model.IdentityTwin;
import com.citizens.digital.twin.domain.model.TransactionEvent;
import org.springframework.stereotype.Service;

@Service
public class TwinSynchronizationService {
  private final IdentityTwinService identityTwinService;

  public TwinSynchronizationService(IdentityTwinService identityTwinService) {
    this.identityTwinService = identityTwinService;
  }

  public IdentityTwin synchronize(IdentityTwin twin, TransactionEvent event) {
    twin.getProfile().apply(event);
    twin.markUpdated();
    return identityTwinService.save(twin);
  }
}
