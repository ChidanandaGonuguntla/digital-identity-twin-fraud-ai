package com.citizens.dti.model;

/**
 * Outcome of reconciling a transaction against the identity twin. Mirrors a propose -> validate ->
 * execute trust boundary: low deviation executes, moderate deviation requires step-up validation,
 * high deviation is rejected.
 */
public enum Decision {
  ALLOW,
  CHALLENGE,
  BLOCK
}
