package com.citizens.digital.twin.infrastructure.persistence.repository;

import com.citizens.digital.twin.infrastructure.persistence.entity.ModelMetricsEntity;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ModelMetricsJpaRepository extends JpaRepository<ModelMetricsEntity, String> {

  @Query(
      value =
          """
          SELECT AVG((feature_snapshot->>'fraudProbability')::double precision)
          FROM identity_twin.model_metrics
          WHERE recorded_at >= :since
          """,
      nativeQuery = true)
  Double averageMlScoreSince(@Param("since") Instant since);

  default Double averageMlScoreLastHour() {
    return averageMlScoreSince(Instant.now().minusSeconds(3600));
  }
}
