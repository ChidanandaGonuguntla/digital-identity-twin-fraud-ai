package com.citizens.digital.twin.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.redis")
public record RedisProperties(boolean enabled, String host, int port, String password) {}
