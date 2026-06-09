package com.citizens.digital.twin.infrastructure.persistence.repository;

import com.citizens.digital.twin.infrastructure.persistence.entity.FeatureStoreValueEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureStoreValueJpaRepository
    extends JpaRepository<FeatureStoreValueEntity, String> {
  List<FeatureStoreValueEntity> findByFeatureNameOrderByUpdatedAtDesc(String featureName);

  @org.springframework.data.jpa.repository.Query(
      "SELECT DISTINCT f.featureName FROM FeatureStoreValueEntity f ORDER BY f.featureName")
  List<String> findDistinctFeatureNames();

  long count();
}
