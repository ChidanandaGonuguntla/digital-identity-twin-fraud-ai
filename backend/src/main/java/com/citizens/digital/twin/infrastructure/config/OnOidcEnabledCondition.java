package com.citizens.digital.twin.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class OnOidcEnabledCondition extends SpringBootCondition {

  @Override
  public ConditionOutcome getMatchOutcome(
      ConditionContext context, AnnotatedTypeMetadata metadata) {
    String issuer = context.getEnvironment().getProperty("app.identity.issuer-uri", "");
    if (issuer.isBlank()) {
      return ConditionOutcome.noMatch("OIDC issuer URI is not configured");
    }
    String provider =
        context.getEnvironment().getProperty("app.identity.provider", "local").trim().toLowerCase();
    boolean enabled =
        "oidc".equals(provider) || "keycloak".equals(provider) || "okta".equals(provider);
    return enabled
        ? ConditionOutcome.match("OIDC identity provider is enabled")
        : ConditionOutcome.noMatch("Identity provider is not OIDC-compatible");
  }
}
