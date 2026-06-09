package com.citizens.digital.twin.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class FraudDecisionFlowIntegrationTest extends AbstractPostgresIntegrationTest {

  @Test
  void evaluateTransaction_persistsDecisionThroughCanonicalPipeline() {
    ResponseEntity<Map> response = postDecision("CUST-IT-001", "TXN-IT-001");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("customerId")).isEqualTo("CUST-IT-001");
    assertThat(response.getBody().get("transactionId")).isEqualTo("TXN-IT-001");
    assertThat(response.getBody().get("decision")).isIn("ALLOW", "CHALLENGE", "BLOCK");
    assertThat(response.getBody().get("assessmentId")).isNotNull();
  }

  @Test
  void invalidRequest_returnsStandardizedValidationError() {
    Map<String, Object> body = Map.of("customerId", "", "amount", -1);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            baseUrl() + "/fraud/decisions", new HttpEntity<>(body, headers), Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("result")).isEqualTo("WARNING");
    assertThat(response.getBody().get("message")).isEqualTo("Validation failed");
    assertThat(response.getBody().get("path")).isEqualTo("/api/v1/fraud/decisions");
  }
}
