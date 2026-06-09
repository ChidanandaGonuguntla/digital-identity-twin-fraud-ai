package com.citizens.digital.twin.infrastructure.persistence.repository;

import com.citizens.digital.twin.infrastructure.persistence.entity.ModelRegistryEntity;
import com.citizens.digital.twin.infrastructure.persistence.entity.ModelRegistryEntity.ModelRegistryId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ModelRegistryJpaRepository
    extends JpaRepository<ModelRegistryEntity, ModelRegistryId> {

  List<ModelRegistryEntity> findAllByOrderByDeployedAtDesc();

  Optional<ModelRegistryEntity> findFirstByActiveTrue();

  @Modifying
  @Query("update ModelRegistryEntity m set m.active = false where m.active = true")
  void deactivateAll();
}
