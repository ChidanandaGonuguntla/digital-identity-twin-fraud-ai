package com.citizens.digital.twin.infrastructure.security;

import com.citizens.digital.twin.infrastructure.config.IdentityProperties;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class OidcJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
  private static final Set<String> KNOWN_ROLES =
      Set.of("FRAUD_ANALYST", "FRAUD_MANAGER", "MODEL_RISK_ADMIN", "AUDITOR", "ADMIN");

  private final IdentityProperties identityProperties;

  public OidcJwtAuthenticationConverter(IdentityProperties identityProperties) {
    this.identityProperties = identityProperties;
  }

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    Collection<GrantedAuthority> authorities = new ArrayList<>();
    for (String role : extractRoles(jwt)) {
      authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
    }
    if (authorities.isEmpty()) {
      authorities.add(new SimpleGrantedAuthority("ROLE_FRAUD_ANALYST"));
    }
    return new JwtAuthenticationToken(jwt, authorities, resolvePrincipal(jwt));
  }

  private Set<String> extractRoles(Jwt jwt) {
    Set<String> roles = new LinkedHashSet<>();
    addClaimValues(roles, jwt.getClaim(identityProperties.effectiveRolesClaim()));
    addClaimValues(roles, jwt.getClaim(identityProperties.effectiveGroupsClaim()));
    Object realmAccess = jwt.getClaim("realm_access");
    if (realmAccess instanceof Map<?, ?> map) {
      Object realmRoles = map.get("roles");
      addClaimValues(roles, realmRoles);
    }
    Object resourceAccess = jwt.getClaim("resource_access");
    if (resourceAccess instanceof Map<?, ?> clients) {
      for (Object client : clients.values()) {
        if (client instanceof Map<?, ?> clientMap) {
          addClaimValues(roles, clientMap.get("roles"));
        }
      }
    }
    Set<String> normalized = new LinkedHashSet<>();
    for (String role : roles) {
      String mapped = normalizeRole(role);
      if (mapped != null) {
        normalized.add(mapped);
      }
    }
    return normalized;
  }

  private void addClaimValues(Set<String> target, Object claim) {
    if (claim == null) {
      return;
    }
    if (claim instanceof String value) {
      if (!value.isBlank()) {
        target.add(value);
      }
      return;
    }
    if (claim instanceof Collection<?> collection) {
      for (Object item : collection) {
        if (item != null && !String.valueOf(item).isBlank()) {
          target.add(String.valueOf(item));
        }
      }
    }
  }

  private String normalizeRole(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String normalized = raw.trim().toUpperCase().replace('-', '_').replace(' ', '_');
    if (normalized.startsWith("ROLE_")) {
      normalized = normalized.substring(5);
    }
    if (KNOWN_ROLES.contains(normalized)) {
      return normalized;
    }
    if (normalized.contains("MODEL") && normalized.contains("RISK")) {
      return "MODEL_RISK_ADMIN";
    }
    if (normalized.contains("FRAUD") && normalized.contains("MANAGER")) {
      return "FRAUD_MANAGER";
    }
    if (normalized.contains("FRAUD") && normalized.contains("ANALYST")) {
      return "FRAUD_ANALYST";
    }
    if (normalized.contains("AUDITOR")) {
      return "AUDITOR";
    }
    if (normalized.contains("ADMIN")) {
      return "ADMIN";
    }
    return null;
  }

  private String resolvePrincipal(Jwt jwt) {
    String email = jwt.getClaimAsString("email");
    if (email != null && !email.isBlank()) {
      return email;
    }
    String preferred = jwt.getClaimAsString("preferred_username");
    if (preferred != null && !preferred.isBlank()) {
      return preferred;
    }
    return jwt.getSubject();
  }
}
