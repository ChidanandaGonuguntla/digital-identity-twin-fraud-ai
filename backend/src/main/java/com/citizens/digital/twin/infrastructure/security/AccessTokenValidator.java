package com.citizens.digital.twin.infrastructure.security;

import com.citizens.digital.twin.infrastructure.config.IdentityProperties;
import com.citizens.digital.twin.infrastructure.config.SecurityProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenValidator {
  private final SecurityProperties securityProperties;
  private final IdentityProperties identityProperties;
  private final JwtService jwtService;
  private final ObjectProvider<JwtDecoder> jwtDecoder;

  public AccessTokenValidator(
      SecurityProperties securityProperties,
      IdentityProperties identityProperties,
      JwtService jwtService,
      ObjectProvider<JwtDecoder> jwtDecoder) {
    this.securityProperties = securityProperties;
    this.identityProperties = identityProperties;
    this.jwtService = jwtService;
    this.jwtDecoder = jwtDecoder;
  }

  public boolean isValid(String token) {
    if (!securityProperties.enabled()) {
      return true;
    }
    if (token == null || token.isBlank()) {
      return false;
    }
    if (identityProperties.oidcEnabled()) {
      JwtDecoder decoder = jwtDecoder.getIfAvailable();
      if (decoder == null) {
        return false;
      }
      try {
        decoder.decode(token);
        return true;
      } catch (Exception ex) {
        return false;
      }
    }
    return jwtService.parse(token).isPresent();
  }
}
