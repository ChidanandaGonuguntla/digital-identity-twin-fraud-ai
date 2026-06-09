package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.api.dto.AuthConfigResponse;
import com.citizens.digital.twin.api.dto.LoginRequest;
import com.citizens.digital.twin.api.dto.LoginResponse;
import com.citizens.digital.twin.infrastructure.config.IdentityProperties;
import com.citizens.digital.twin.infrastructure.config.SecurityProperties;
import com.citizens.digital.twin.infrastructure.config.SecurityProperties.SecurityUser;
import com.citizens.digital.twin.infrastructure.security.JwtService;
import com.citizens.digital.twin.shared.exception.BusinessException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private final SecurityProperties securityProperties;
  private final IdentityProperties identityProperties;
  private final JwtService jwtService;

  public AuthService(
      SecurityProperties securityProperties,
      IdentityProperties identityProperties,
      JwtService jwtService) {
    this.securityProperties = securityProperties;
    this.identityProperties = identityProperties;
    this.jwtService = jwtService;
  }

  public AuthConfigResponse config() {
    return new AuthConfigResponse(
        securityProperties.enabled(),
        identityProperties.oidcEnabled() ? "oidc" : "local",
        identityProperties.issuerUri(),
        identityProperties.audience(),
        identityProperties.jwksUri());
  }

  public LoginResponse login(LoginRequest request) {
    if (identityProperties.oidcEnabled()) {
      throw new BusinessException("Local login disabled when OIDC provider is active");
    }
    var users =
        securityProperties.users() == null ? List.<SecurityUser>of() : securityProperties.users();
    SecurityUser user =
        users.stream()
            .filter(
                candidate ->
                    candidate.email().equalsIgnoreCase(request.email())
                        && candidate.password().equals(request.password()))
            .findFirst()
            .orElseThrow(() -> new BusinessException("Invalid email or password"));
    String token = jwtService.createToken(user.email(), user.role());
    return new LoginResponse(
        token, "Bearer", jwtService.expirationSeconds(), user.email(), user.role());
  }
}
