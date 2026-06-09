package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.domain.model.IdentityTwin;
import com.citizens.digital.twin.domain.model.RiskAssessment;
import com.citizens.digital.twin.domain.model.TransactionEvent;
import com.citizens.digital.twin.domain.scoring.TwinDeviationScoringService.TwinDeviationScore;
import com.citizens.digital.twin.infrastructure.config.ScoringProperties;
import com.citizens.digital.twin.infrastructure.persistence.entity.TwinDriftEventEntity;
import com.citizens.digital.twin.infrastructure.persistence.repository.TwinDriftEventJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TwinDriftService {
  private final TwinDriftEventJpaRepository repository;
  private final ScoringProperties scoringProperties;
  private final ObjectMapper objectMapper;

  public TwinDriftService(
      TwinDriftEventJpaRepository repository,
      ScoringProperties scoringProperties,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.scoringProperties = scoringProperties;
    this.objectMapper = objectMapper;
  }

  public Optional<TwinDriftEventEntity> recordIfThreshold(
      TransactionEvent event,
      RiskAssessment assessment,
      IdentityTwin twin,
      TwinDeviationScore twinScore) {
    BigDecimal driftScore = twinScore.score();
    BigDecimal threshold = BigDecimal.valueOf(scoringProperties.driftThreshold());
    if (driftScore.compareTo(threshold) < 0) {
      return Optional.empty();
    }
    TwinDriftEventEntity entity = new TwinDriftEventEntity();
    entity.setDriftEventId(UUID.randomUUID().toString());
    entity.setAssessmentId(assessment.assessmentId());
    entity.setCustomerId(event.customerId());
    entity.setTransactionId(event.transactionId());
    entity.setDriftScore(driftScore);
    entity.setDriftThreshold(threshold);
    entity.setSignalSnapshot(write(twinScore.signals()));
    entity.setBaselineSnapshot(
        write(
            Map.of(
                "transactionCount",
                twin.getProfile().getTransactionCount(),
                "knownDevices",
                twin.getProfile().getKnownDevices(),
                "usualCountries",
                twin.getProfile().getUsualCountries())));
    return Optional.of(repository.save(entity));
  }

  private String write(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception ex) {
      return "{}";
    }
  }
}
