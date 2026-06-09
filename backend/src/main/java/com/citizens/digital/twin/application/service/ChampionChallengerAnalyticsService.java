package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.api.dto.ChampionChallengerSummaryResponse;
import com.citizens.digital.twin.infrastructure.config.MlModelProperties;
import com.citizens.digital.twin.infrastructure.persistence.repository.FraudDecisionAuditJpaRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;

@Service
public class ChampionChallengerAnalyticsService {
  private final FraudDecisionAuditJpaRepository auditRepository;
  private final MlModelProperties mlModelProperties;

  public ChampionChallengerAnalyticsService(
      FraudDecisionAuditJpaRepository auditRepository, MlModelProperties mlModelProperties) {
    this.auditRepository = auditRepository;
    this.mlModelProperties = mlModelProperties;
  }

  public ChampionChallengerSummaryResponse summary(int hours) {
    Instant since = Instant.now().minus(Math.max(1, hours), ChronoUnit.HOURS);
    Object[] row = auditRepository.championChallengerAggregate(since);
    long scored = row == null || row[0] == null ? 0L : ((Number) row[0]).longValue();
    double avgChampion = row == null || row[1] == null ? 0.0 : ((Number) row[1]).doubleValue();
    double avgChallenger = row == null || row[2] == null ? 0.0 : ((Number) row[2]).doubleValue();
    double avgDelta = row == null || row[3] == null ? 0.0 : ((Number) row[3]).doubleValue();
    double agreement = row == null || row[4] == null ? 0.0 : ((Number) row[4]).doubleValue();
    return new ChampionChallengerSummaryResponse(
        mlModelProperties.championVersion(),
        mlModelProperties.challengerVersion(),
        scored,
        avgChampion,
        avgChallenger,
        avgDelta,
        agreement);
  }
}
