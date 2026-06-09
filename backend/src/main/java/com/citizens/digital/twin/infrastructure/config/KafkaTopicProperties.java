package com.citizens.digital.twin.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fraud.kafka")
public record KafkaTopicProperties(
    boolean enabled,
    String transactionTopic,
    String decisionTopic,
    String auditTopic,
    String stepUpTopic,
    String twinDriftTopic,
    String transactionDltTopic,
    String decisionDltTopic,
    int consumerRetryAttempts,
    long consumerRetryIntervalMs) {}
