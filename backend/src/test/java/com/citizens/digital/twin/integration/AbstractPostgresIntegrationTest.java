package com.citizens.digital.twin.integration;

import com.citizens.digital.twin.DigitalIdentityTwinApplication;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = DigitalIdentityTwinApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
abstract class AbstractPostgresIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("digital_twin_db")
          .withUsername("postgres")
          .withPassword("admin");

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url", () -> POSTGRES.getJdbcUrl() + "?currentSchema=identity_twin");
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("fraud.kafka.enabled", () -> "false");
  }

  @LocalServerPort protected int port;

  @Autowired protected TestRestTemplate restTemplate;

  protected String baseUrl() {
    return "http://localhost:" + port + "/api/v1";
  }

  protected ResponseEntity<Map> postDecision(String customerId, String transactionId) {
    Map<String, Object> body = new HashMap<>();
    body.put("customerId", customerId);
    body.put("transactionId", transactionId);
    body.put("amount", 42.5);
    body.put("currency", "USD");
    body.put("merchantCategory", "GROCERY");
    body.put("deviceId", "dev-it-1");
    body.put("channel", "MOBILE");
    body.put("latitude", 35.2271);
    body.put("longitude", -80.8431);
    body.put("countryCode", "US");
    body.put("timestamp", Instant.parse("2026-06-08T12:00:00Z").toString());
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return restTemplate.postForEntity(
        baseUrl() + "/fraud/decisions", new HttpEntity<>(body, headers), Map.class);
  }
}
