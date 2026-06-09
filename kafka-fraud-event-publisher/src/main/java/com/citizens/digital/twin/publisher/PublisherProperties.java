package com.citizens.digital.twin.publisher;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "publisher")
public record PublisherProperties(
    String topic,
    String datasetPath,
    int publishRatePerSecond,
    int maxRecords,
    boolean publishOnStartup) {}
