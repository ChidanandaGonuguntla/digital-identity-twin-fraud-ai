package com.citizens.digital.twin.domain.ml;

public enum ModelRegistryStatus {
  PENDING_APPROVAL,
  APPROVED,
  REJECTED,
  CANDIDATE,
  ACTIVE,
  DEPRECATED,
  RETIRED,
  ROLLED_BACK
}
