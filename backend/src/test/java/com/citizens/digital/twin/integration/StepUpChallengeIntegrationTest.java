package com.citizens.digital.twin.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class StepUpChallengeIntegrationTest extends AbstractPostgresIntegrationTest {

  @Test
  void challengeWorkflow_createApproveAndList() {
    ResponseEntity<Map> decision = postDecision("CUST-STEP-001", "TXN-STEP-001");
    assertThat(decision.getStatusCode()).isEqualTo(HttpStatus.OK);
    String assessmentId = decision.getBody().get("assessmentId").toString();

    Map<String, Object> createBody =
        Map.of(
            "assessmentId",
            assessmentId,
            "customerId",
            "CUST-STEP-001",
            "transactionId",
            "TXN-STEP-001",
            "channel",
            "WEB",
            "reason",
            "Integration test challenge");
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    ResponseEntity<Map> created =
        restTemplate.postForEntity(
            baseUrl() + "/challenges", new HttpEntity<>(createBody, headers), Map.class);
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
    String challengeId = created.getBody().get("challengeId").toString();

    ResponseEntity<Map> listed =
        restTemplate.getForEntity(baseUrl() + "/challenges?status=PENDING", Map.class);
    assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listed.getBody().get("totalElements")).isNotNull();

    ResponseEntity<Map> approved =
        restTemplate.postForEntity(
            baseUrl() + "/challenges/" + challengeId + "/approve", HttpEntity.EMPTY, Map.class);
    assertThat(approved.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(approved.getBody().get("status")).isEqualTo("APPROVED");
  }
}
