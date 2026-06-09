package com.citizens.digital.twin.domain.ml.onnx;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.citizens.digital.twin.infrastructure.config.MlModelProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class OnnxModelSessionManager {
  private static final Logger log = LoggerFactory.getLogger(OnnxModelSessionManager.class);

  private final MlModelProperties properties;
  private final ResourceLoader resourceLoader;
  private final ObjectMapper objectMapper;

  private final OrtEnvironment environment = OrtEnvironment.getEnvironment();

  private volatile LoadedModel loadedModel;
  private volatile LoadedModel loadedChallengerModel;

  public OnnxModelSessionManager(
      MlModelProperties properties, ResourceLoader resourceLoader, ObjectMapper objectMapper) {
    this.properties = properties;
    this.resourceLoader = resourceLoader;
    this.objectMapper = objectMapper;
  }

  public synchronized void loadActiveModel() {
    loadVersion(properties.activeVersion());
    if (properties.challengerEnabled()) {
      loadChallengerVersion(properties.challengerVersion());
    }
  }

  public synchronized void loadChallengerVersion(String version) {
    if (loadedChallengerModel != null && version.equals(loadedChallengerModel.version())) {
      return;
    }
    closeChallenger();
    try {
      OnnxModelArtifact artifact = readArtifact(version);
      Resource onnxResource = resourceLoader.getResource(resolveOnnxPath(artifact.onnxFile()));
      try (InputStream inputStream = onnxResource.getInputStream()) {
        OrtSession session =
            environment.createSession(inputStream.readAllBytes(), new OrtSession.SessionOptions());
        loadedChallengerModel = new LoadedModel(version, artifact, session);
        log.info("Loaded ONNX challenger model version={}", version);
      }
    } catch (Exception ex) {
      loadedChallengerModel = null;
      log.warn("Failed to load ONNX challenger model version={}", version, ex);
    }
  }

  public boolean isChallengerReady() {
    return loadedChallengerModel != null;
  }

  public double predictChallenger(float[] featureVector) throws OrtException {
    LoadedModel model = loadedChallengerModel;
    if (model == null) {
      throw new IllegalStateException("ONNX challenger model is not loaded");
    }
    try (OnnxTensor inputTensor =
            OnnxTensor.createTensor(
                environment,
                FloatBuffer.wrap(featureVector),
                new long[] {1, featureVector.length});
        OrtSession.Result result =
            model.session().run(java.util.Map.of(model.artifact().inputName(), inputTensor))) {
      return extractProbability(result, model.artifact().outputName());
    }
  }

  public Optional<LoadedModel> currentChallengerModel() {
    return Optional.ofNullable(loadedChallengerModel);
  }

  public synchronized void rollback() {
    loadVersion(properties.rollbackVersion());
  }

  public synchronized void loadVersion(String version) {
    closeCurrent();
    try {
      OnnxModelArtifact artifact = readArtifact(version);
      Resource onnxResource = resourceLoader.getResource(resolveOnnxPath(artifact.onnxFile()));
      try (InputStream inputStream = onnxResource.getInputStream()) {
        OrtSession session =
            environment.createSession(inputStream.readAllBytes(), new OrtSession.SessionOptions());
        loadedModel = new LoadedModel(version, artifact, session);
        log.info("Loaded ONNX fraud model version={} type={}", version, artifact.modelType());
      }
    } catch (Exception ex) {
      loadedModel = null;
      log.error("Failed to load ONNX model version={}", version, ex);
    }
  }

  public boolean isReady() {
    return loadedModel != null;
  }

  public Optional<LoadedModel> currentModel() {
    return Optional.ofNullable(loadedModel);
  }

  public double predict(float[] featureVector) throws OrtException {
    LoadedModel model = loadedModel == null ? null : loadedModel;
    if (model == null) {
      throw new IllegalStateException("ONNX model is not loaded");
    }
    try (OnnxTensor inputTensor =
            OnnxTensor.createTensor(
                environment,
                FloatBuffer.wrap(featureVector),
                new long[] {1, featureVector.length});
        OrtSession.Result result =
            model.session().run(java.util.Map.of(model.artifact().inputName(), inputTensor))) {
      return extractProbability(result, model.artifact().outputName());
    }
  }

  private double extractProbability(OrtSession.Result result, String preferredOutput)
      throws OrtException {
    if (preferredOutput != null && !preferredOutput.isBlank()) {
      Optional<OnnxValue> preferred = result.get(preferredOutput);
      if (preferred.isPresent()) {
        return readProbability(asTensor(preferred.get()));
      }
    }
    for (var entry : result) {
      if (entry.getKey().toLowerCase().contains("prob")) {
        return readProbability(asTensor(entry.getValue()));
      }
    }
    return readProbability(asTensor(result.get(0)));
  }

  private OnnxTensor asTensor(OnnxValue value) {
    if (value instanceof OnnxTensor tensor) {
      return tensor;
    }
    throw new IllegalStateException("Expected OnnxTensor model output");
  }

  private double readProbability(OnnxTensor tensor) throws OrtException {
    Object value = tensor.getValue();
    if (value instanceof float[][] matrix && matrix.length > 0) {
      float[] row = matrix[0];
      return row.length > 1 ? row[1] : row[0];
    }
    if (value instanceof float[] vector && vector.length > 0) {
      return vector.length > 1 ? vector[1] : vector[0];
    }
    if (value instanceof double[][] matrix && matrix.length > 0) {
      double[] row = matrix[0];
      return row.length > 1 ? row[1] : row[0];
    }
    throw new IllegalStateException("Unsupported ONNX output tensor shape");
  }

  private OnnxModelArtifact readArtifact(String version) throws Exception {
    Resource metadataResource =
        resourceLoader.getResource(properties.modelsDirectory() + "/" + version + ".metadata.json");
    try (InputStream inputStream = metadataResource.getInputStream()) {
      return objectMapper.readValue(inputStream, OnnxModelArtifact.class);
    }
  }

  private String resolveOnnxPath(String onnxFile) {
    if (onnxFile.startsWith("classpath:") || onnxFile.startsWith("file:")) {
      return onnxFile;
    }
    return properties.modelsDirectory() + "/" + onnxFile;
  }

  private synchronized void closeCurrent() {
    if (loadedModel != null) {
      try {
        loadedModel.session().close();
      } catch (OrtException ex) {
        log.warn("Failed to close ONNX session for version={}", loadedModel.version(), ex);
      }
      loadedModel = null;
    }
  }

  private synchronized void closeChallenger() {
    if (loadedChallengerModel != null) {
      try {
        loadedChallengerModel.session().close();
      } catch (OrtException ex) {
        log.warn(
            "Failed to close ONNX challenger session for version={}",
            loadedChallengerModel.version(),
            ex);
      }
      loadedChallengerModel = null;
    }
  }

  public record LoadedModel(String version, OnnxModelArtifact artifact, OrtSession session) {}
}
