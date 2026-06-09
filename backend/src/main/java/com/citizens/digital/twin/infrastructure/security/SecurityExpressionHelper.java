package com.citizens.digital.twin.infrastructure.security;

import com.citizens.digital.twin.infrastructure.config.SecurityProperties;
import org.springframework.stereotype.Component;

@Component("securityExpressions")
public class SecurityExpressionHelper {
  private final SecurityProperties securityProperties;

  public SecurityExpressionHelper(SecurityProperties securityProperties) {
    this.securityProperties = securityProperties;
  }

  public boolean permitIfDisabled() {
    return !securityProperties.enabled();
  }
}
