package com.citizens.digital.twin.infrastructure.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
    boolean enabled, String jwtSecret, int jwtExpirationMinutes, List<SecurityUser> users) {

  public record SecurityUser(String email, String password, String role) {}
}
