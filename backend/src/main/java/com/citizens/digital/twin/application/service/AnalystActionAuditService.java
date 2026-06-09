package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.infrastructure.persistence.entity.AnalystActionAuditEntity;
import com.citizens.digital.twin.infrastructure.persistence.repository.AnalystActionAuditJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AnalystActionAuditService {
  private final AnalystActionAuditJpaRepository repository;
  private final ObjectMapper objectMapper;

  public AnalystActionAuditService(
      AnalystActionAuditJpaRepository repository, ObjectMapper objectMapper) {
    this.repository = repository;
    this.objectMapper = objectMapper;
  }

  public void record(
      String actionType, String resourceType, String resourceId, Map<String, Object> details) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String actorEmail =
        authentication == null || authentication.getPrincipal() == null
            ? "system"
            : String.valueOf(authentication.getPrincipal());
    String actorRole = resolveRole(authentication);

    AnalystActionAuditEntity entity = new AnalystActionAuditEntity();
    entity.setActionId(UUID.randomUUID().toString());
    entity.setActorEmail(actorEmail);
    entity.setActorRole(actorRole);
    entity.setActionType(actionType);
    entity.setResourceType(resourceType);
    entity.setResourceId(resourceId);
    entity.setDetailsJson(write(details == null ? Map.of() : details));
    repository.save(entity);
  }

  private String resolveRole(Authentication authentication) {
    if (authentication == null || authentication.getAuthorities() == null) {
      return "SYSTEM";
    }
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .filter(a -> a.startsWith("ROLE_"))
        .map(a -> a.substring("ROLE_".length()))
        .findFirst()
        .orElse("UNKNOWN");
  }

  private String write(Map<String, Object> details) {
    try {
      return objectMapper.writeValueAsString(new LinkedHashMap<>(details));
    } catch (Exception ex) {
      return "{}";
    }
  }
}
