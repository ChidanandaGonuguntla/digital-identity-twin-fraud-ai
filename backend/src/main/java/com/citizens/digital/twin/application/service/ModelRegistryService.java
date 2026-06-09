package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.domain.ml.ModelRegistryStatus;
import com.citizens.digital.twin.domain.ml.onnx.OnnxModelArtifact;
import com.citizens.digital.twin.domain.ml.onnx.OnnxModelSessionManager;
import com.citizens.digital.twin.infrastructure.config.MlModelProperties;
import com.citizens.digital.twin.infrastructure.persistence.entity.ModelRegistryEntity;
import com.citizens.digital.twin.infrastructure.persistence.repository.ModelRegistryJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModelRegistryService {
  private static final Logger log = LoggerFactory.getLogger(ModelRegistryService.class);

  private final MlModelProperties properties;
  private final OnnxModelSessionManager sessionManager;
  private final ModelRegistryJpaRepository repository;
  private final ObjectMapper objectMapper;

  private volatile String activeVersion;
  private volatile boolean onnxActive;

  public ModelRegistryService(
      MlModelProperties properties,
      OnnxModelSessionManager sessionManager,
      ModelRegistryJpaRepository repository,
      ObjectMapper objectMapper) {
    this.properties = properties;
    this.sessionManager = sessionManager;
    this.repository = repository;
    this.objectMapper = objectMapper;
  }

  @PostConstruct
  void initialize() {
    activeVersion = properties.activeVersion();
    if (useOnnxProvider()) {
      sessionManager.loadActiveModel();
      onnxActive = sessionManager.isReady();
      if (onnxActive) {
        syncRegistryFromArtifact(activeVersion);
      } else {
        log.warn(
            "ONNX provider configured but model {} is unavailable. Falling back to heuristic.",
            activeVersion);
      }
    }
  }

  public boolean useOnnx() {
    return useOnnxProvider() && onnxActive;
  }

  public String activeVersion() {
    return activeVersion;
  }

  public Map<String, Object> status() {
    Map<String, Object> status = new LinkedHashMap<>();
    status.put("provider", properties.provider());
    status.put("activeVersion", activeVersion);
    status.put("rollbackVersion", properties.rollbackVersion());
    status.put("onnxLoaded", onnxActive);
    status.put("modelsDirectory", properties.modelsDirectory());
    return status;
  }

  public List<String> versions() {
    return List.of(properties.activeVersion(), properties.rollbackVersion());
  }

  @Transactional
  public Map<String, Object> reloadActive() {
    return activateVersion(properties.activeVersion());
  }

  @Transactional
  public Map<String, Object> rollback() {
    String previousVersion = activeVersion;
    Map<String, Object> result = activateVersion(properties.rollbackVersion());
    if (previousVersion != null && !previousVersion.equals(properties.rollbackVersion())) {
      markRolledBack(previousVersion);
    }
    return result;
  }

  public void retireVersion(String version) {
    ModelRegistryEntity entity = requireEntity(version);
    if (entity.isActive()) {
      throw new IllegalStateException(
          "Cannot retire the active model. Activate a replacement first.");
    }
    entity.setRegistryStatus(ModelRegistryStatus.RETIRED.name());
    entity.setModelRole("RETIRED");
    entity.setActive(false);
    repository.save(entity);
  }

  @Transactional
  public Map<String, Object> promoteChallengerToChampion() {
    if (!properties.challengerEnabled()) {
      throw new IllegalStateException("Champion/challenger mode is disabled");
    }
    String challenger = properties.challengerVersion();
    return activateVersion(challenger);
  }

  @Transactional
  public Map<String, Object> activateVersion(String version) {
    requireDeployable(version);
    if (!useOnnxProvider()) {
      onnxActive = false;
      activeVersion = version;
      markActive(version);
      return status();
    }
    sessionManager.loadVersion(version);
    activeVersion = version;
    onnxActive = sessionManager.isReady();
    if (onnxActive) {
      markActive(version);
      syncRegistryFromArtifact(version);
    } else {
      log.warn("ONNX model {} unavailable. Using heuristic fallback.", version);
    }
    return status();
  }

  private boolean useOnnxProvider() {
    return "onnx".equalsIgnoreCase(properties.provider());
  }

  private void markActive(String version) {
    for (ModelRegistryEntity existing : repository.findAllByOrderByDeployedAtDesc()) {
      if (existing.isActive()) {
        existing.setActive(false);
        if (ModelRegistryStatus.ACTIVE.name().equals(existing.getRegistryStatus())) {
          existing.setRegistryStatus(ModelRegistryStatus.DEPRECATED.name());
        }
        repository.save(existing);
      }
    }
    ModelRegistryEntity entity = requireEntity(version);
    entity.setActive(true);
    entity.setRegistryStatus(ModelRegistryStatus.ACTIVE.name());
    entity.setModelRole("CHAMPION");
    entity.setDeployedAt(Instant.now());
    repository.save(entity);
    for (ModelRegistryEntity candidate : repository.findAllByOrderByDeployedAtDesc()) {
      if (!version.equals(candidate.getModelVersion())
          && properties.challengerVersion().equals(candidate.getModelVersion())) {
        candidate.setModelRole("CHALLENGER");
        candidate.setRegistryStatus(ModelRegistryStatus.CANDIDATE.name());
        repository.save(candidate);
      }
    }
  }

  private void markRolledBack(String version) {
    ModelRegistryEntity entity = requireEntity(version);
    entity.setActive(false);
    entity.setRegistryStatus(ModelRegistryStatus.ROLLED_BACK.name());
    repository.save(entity);
  }

  private void requireDeployable(String version) {
    ModelRegistryEntity entity = requireEntity(version);
    String status = entity.getRegistryStatus();
    if (!ModelRegistryStatus.APPROVED.name().equals(status)
        && !ModelRegistryStatus.ACTIVE.name().equals(status)) {
      throw new IllegalStateException(
          "Model version " + version + " requires approval before deployment");
    }
  }

  private ModelRegistryEntity requireEntity(String version) {
    ModelRegistryEntity.ModelRegistryId key = new ModelRegistryEntity.ModelRegistryId();
    key.setModelName("digital-twin-fraud-risk");
    key.setModelVersion(version);
    return repository
        .findById(key)
        .orElseGet(
            () -> {
              ModelRegistryEntity created = new ModelRegistryEntity();
              created.setId(key);
              created.setArtifactPath(properties.modelsDirectory() + "/" + version + ".onnx");
              created.setFeatureOrderJson("[]");
              created.setRegistryStatus(ModelRegistryStatus.APPROVED.name());
              created.setDeployedAt(Instant.now());
              return created;
            });
  }

  private void syncRegistryFromArtifact(String version) {
    sessionManager
        .currentModel()
        .ifPresent(
            loaded -> {
              try {
                OnnxModelArtifact artifact = loaded.artifact();
                ModelRegistryEntity entity = requireEntity(version);
                entity.setMetricsJson(objectMapper.writeValueAsString(artifact.metrics()));
                entity.setFeatureOrderJson(
                    objectMapper.writeValueAsString(artifact.featureOrder()));
                entity.setTrainingDatasetVersion(artifact.trainingDatasetVersion());
                entity.setFeatureSchemaVersion(artifact.featureSchemaVersion());
                entity.setTrainedAt(artifact.trainedAt());
                entity.setArtifactPath(properties.modelsDirectory() + "/" + artifact.onnxFile());
                repository.save(entity);
              } catch (JsonProcessingException ex) {
                log.warn("Failed to sync model registry metadata for version={}", version, ex);
              }
            });
  }
}
