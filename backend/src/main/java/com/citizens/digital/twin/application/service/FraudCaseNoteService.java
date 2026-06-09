package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.infrastructure.persistence.entity.FraudCaseNoteEntity;
import com.citizens.digital.twin.infrastructure.persistence.repository.FraudCaseNoteJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class FraudCaseNoteService {
  private final FraudCaseNoteJpaRepository repository;
  private final ObjectMapper objectMapper;

  public FraudCaseNoteService(FraudCaseNoteJpaRepository repository, ObjectMapper objectMapper) {
    this.repository = repository;
    this.objectMapper = objectMapper;
  }

  public void saveAnalystNote(
      String challengeId,
      String assessmentId,
      String customerId,
      String author,
      String noteBody,
      Map<String, Object> metadata) {
    FraudCaseNoteEntity entity = new FraudCaseNoteEntity();
    entity.setNoteId(UUID.randomUUID().toString());
    entity.setChallengeId(challengeId);
    entity.setAssessmentId(assessmentId);
    entity.setCustomerId(customerId);
    entity.setAuthor(author == null || author.isBlank() ? "analyst" : author);
    entity.setNoteType("ANALYST");
    entity.setNoteBody(noteBody);
    entity.setMetadataJson(write(metadata == null ? Map.of() : metadata));
    repository.save(entity);
  }

  private String write(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception ex) {
      return "{}";
    }
  }
}
