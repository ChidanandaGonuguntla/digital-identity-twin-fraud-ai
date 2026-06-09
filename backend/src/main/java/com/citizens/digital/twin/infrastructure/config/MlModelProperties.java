package com.citizens.digital.twin.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dti.ml")
public record MlModelProperties(
    String provider,
    String modelsDirectory,
    String activeVersion,
    String rollbackVersion,
    String championVersion,
    String challengerVersion,
    boolean challengerEnabled,
    double driftBaselineScore,
    double driftAlertThreshold) {}
