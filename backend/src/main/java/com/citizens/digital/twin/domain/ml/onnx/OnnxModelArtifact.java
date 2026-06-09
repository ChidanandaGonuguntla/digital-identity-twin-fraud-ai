package com.citizens.digital.twin.domain.ml.onnx;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record OnnxModelArtifact(
    String modelName,
    String modelVersion,
    String modelType,
    String trainingDatasetVersion,
    String featureSchemaVersion,
    String onnxFile,
    String inputName,
    String outputName,
    List<String> featureOrder,
    Map<String, Double> metrics,
    Instant trainedAt,
    double baselineMeanScore) {}
