package com.citizens.digital.twin.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.identity")
public record IdentityProperties(
    String provider,
    String issuerUri,
    String audience,
    String jwksUri,
    String rolesClaim,
    String groupsClaim) {

  public boolean oidcEnabled() {
    if (issuerUri == null || issuerUri.isBlank()) {
      return false;
    }
    String mode = provider == null ? "local" : provider.trim().toLowerCase();
    return "oidc".equals(mode) || "keycloak".equals(mode) || "okta".equals(mode);
  }

  public String effectiveRolesClaim() {
    return rolesClaim == null || rolesClaim.isBlank() ? "roles" : rolesClaim;
  }

  public String effectiveGroupsClaim() {
    return groupsClaim == null || groupsClaim.isBlank() ? "groups" : groupsClaim;
  }
}
