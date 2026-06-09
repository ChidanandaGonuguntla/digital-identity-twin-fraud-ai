package com.citizens.digital.twin.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

class SecurityApiIntegrationTest extends AbstractPostgresIntegrationTest {

  @DynamicPropertySource
  static void enableSecurity(DynamicPropertyRegistry registry) {
    registry.add("app.security.enabled", () -> "true");
  }

  @Test
  void protectedEndpoint_requiresAuthentication() {
    ResponseEntity<Map> response =
        restTemplate.getForEntity(baseUrl() + "/audit/decisions/summary", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("result")).isEqualTo("UNAUTHORIZED");
  }

  @Test
  void login_issuesTokenForValidCredentials() {
    Map<String, String> body = Map.of("email", "analyst@citizens.com", "password", "password");
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            baseUrl() + "/auth/login", new HttpEntity<>(body, headers), Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("accessToken")).isNotNull();
    assertThat(response.getBody().get("role")).isEqualTo("FRAUD_ANALYST");
  }

  @Test
  void protectedEndpoint_allowsBearerToken() {
    String token = loginToken();
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    ResponseEntity<Map> response =
        restTemplate.exchange(
            baseUrl() + "/audit/decisions/summary",
            org.springframework.http.HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
  }

  private String loginToken() {
    Map<String, String> body = Map.of("email", "analyst@citizens.com", "password", "password");
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            baseUrl() + "/auth/login", new HttpEntity<>(body, headers), Map.class);
    return response.getBody().get("accessToken").toString();
  }
}
