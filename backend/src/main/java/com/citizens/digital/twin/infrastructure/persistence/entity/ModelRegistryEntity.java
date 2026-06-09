package com.citizens.digital.twin.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "model_registry", schema = "identity_twin")
public class ModelRegistryEntity {

  @EmbeddedId private ModelRegistryId id = new ModelRegistryId();

  @Column(name = "artifact_path", nullable = false)
  private String artifactPath;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "feature_order", nullable = false, columnDefinition = "jsonb")
  private String featureOrderJson;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metrics", columnDefinition = "jsonb")
  private String metricsJson;

  @Column(name = "trained_at")
  private Instant trainedAt;

  @Column(name = "deployed_at", nullable = false)
  private Instant deployedAt;

  @Column(name = "active", nullable = false)
  private boolean active;

  @Column(name = "training_dataset_version", length = 80)
  private String trainingDatasetVersion;

  @Column(name = "feature_schema_version", length = 80)
  private String featureSchemaVersion;

  @Column(name = "registry_status", nullable = false, length = 40)
  private String registryStatus = "APPROVED";

  @Column(name = "approved_by", length = 255)
  private String approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "bias_review_json", columnDefinition = "jsonb")
  private String biasReviewJson;

  @Column(name = "rejection_reason", columnDefinition = "text")
  private String rejectionReason;

  @Column(name = "model_role", nullable = false, length = 20)
  private String modelRole = "CANDIDATE";

  public ModelRegistryId getId() {
    return id;
  }

  public void setId(ModelRegistryId id) {
    this.id = id;
  }

  public String getModelName() {
    return id.getModelName();
  }

  public void setModelName(String modelName) {
    id.setModelName(modelName);
  }

  public String getModelVersion() {
    return id.getModelVersion();
  }

  public void setModelVersion(String modelVersion) {
    id.setModelVersion(modelVersion);
  }

  public String getArtifactPath() {
    return artifactPath;
  }

  public void setArtifactPath(String artifactPath) {
    this.artifactPath = artifactPath;
  }

  public String getFeatureOrderJson() {
    return featureOrderJson;
  }

  public void setFeatureOrderJson(String featureOrderJson) {
    this.featureOrderJson = featureOrderJson;
  }

  public String getMetricsJson() {
    return metricsJson;
  }

  public void setMetricsJson(String metricsJson) {
    this.metricsJson = metricsJson;
  }

  public Instant getTrainedAt() {
    return trainedAt;
  }

  public void setTrainedAt(Instant trainedAt) {
    this.trainedAt = trainedAt;
  }

  public Instant getDeployedAt() {
    return deployedAt;
  }

  public void setDeployedAt(Instant deployedAt) {
    this.deployedAt = deployedAt;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public String getTrainingDatasetVersion() {
    return trainingDatasetVersion;
  }

  public void setTrainingDatasetVersion(String trainingDatasetVersion) {
    this.trainingDatasetVersion = trainingDatasetVersion;
  }

  public String getFeatureSchemaVersion() {
    return featureSchemaVersion;
  }

  public void setFeatureSchemaVersion(String featureSchemaVersion) {
    this.featureSchemaVersion = featureSchemaVersion;
  }

  public String getRegistryStatus() {
    return registryStatus;
  }

  public void setRegistryStatus(String registryStatus) {
    this.registryStatus = registryStatus;
  }

  public String getApprovedBy() {
    return approvedBy;
  }

  public void setApprovedBy(String approvedBy) {
    this.approvedBy = approvedBy;
  }

  public Instant getApprovedAt() {
    return approvedAt;
  }

  public void setApprovedAt(Instant approvedAt) {
    this.approvedAt = approvedAt;
  }

  public String getBiasReviewJson() {
    return biasReviewJson;
  }

  public void setBiasReviewJson(String biasReviewJson) {
    this.biasReviewJson = biasReviewJson;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }

  public void setRejectionReason(String rejectionReason) {
    this.rejectionReason = rejectionReason;
  }

  public String getModelRole() {
    return modelRole;
  }

  public void setModelRole(String modelRole) {
    this.modelRole = modelRole;
  }

  @Embeddable
  public static class ModelRegistryId implements Serializable {
    @Column(name = "model_name")
    private String modelName;

    @Column(name = "model_version")
    private String modelVersion;

    public String getModelName() {
      return modelName;
    }

    public void setModelName(String modelName) {
      this.modelName = modelName;
    }

    public String getModelVersion() {
      return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
      this.modelVersion = modelVersion;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ModelRegistryId that = (ModelRegistryId) o;
      return Objects.equals(modelName, that.modelName)
          && Objects.equals(modelVersion, that.modelVersion);
    }

    @Override
    public int hashCode() {
      return Objects.hash(modelName, modelVersion);
    }
  }
}
