package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.domain.model.IdentityTwin;
import com.citizens.digital.twin.infrastructure.persistence.entity.IdentityTwinEntity;
import com.citizens.digital.twin.infrastructure.persistence.mapper.IdentityTwinMapper;
import com.citizens.digital.twin.infrastructure.persistence.repository.IdentityTwinJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityTwinService {
  private final IdentityTwinJpaRepository repository;
  private final IdentityTwinMapper mapper;

  public IdentityTwinService(IdentityTwinJpaRepository repository, IdentityTwinMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public IdentityTwin getOrCreateTwin(String customerId) {
    return repository
        .findById(customerId)
        .map(mapper::toDomain)
        .orElseGet(() -> IdentityTwin.newTwin(customerId));
  }

  @Transactional
  public IdentityTwin save(IdentityTwin twin) {
    IdentityTwinEntity entity =
        repository.findById(twin.getCustomerId()).orElseGet(IdentityTwinEntity::new);
    mapper.applyToEntity(entity, twin);
    return mapper.toDomain(repository.save(entity));
  }
}
