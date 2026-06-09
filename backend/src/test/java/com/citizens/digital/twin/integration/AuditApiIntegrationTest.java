package com.citizens.digital.twin.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AuditApiIntegrationTest extends AbstractPostgresIntegrationTest {

  @BeforeEach
  void seedAuditRow() {
    ResponseEntity<Map> decision = postDecision("CUST-AUDIT-001", "TXN-AUDIT-001");
    assertThat(decision.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void summary_returnsAggregatedMetrics() {
    ResponseEntity<Map> response =
        restTemplate.getForEntity(baseUrl() + "/audit/decisions/summary", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("totalEvents")).isNotNull();
    assertThat(((Number) response.getBody().get("totalEvents")).longValue()).isGreaterThan(0L);
  }

  @Test
  void page_returnsPaginatedDecisions() {
    ResponseEntity<Map> response =
        restTemplate.getForEntity(baseUrl() + "/audit/decisions?page=0&size=10", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("items")).isInstanceOf(List.class);
  }

  @Test
  void page_returnsMerchantCategoryAndAmountFromSnapshot() {
    ResponseEntity<Map> response =
        restTemplate.getForEntity(baseUrl() + "/audit/decisions?page=0&size=10", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<Map<String, Object>> items = (List<Map<String, Object>>) response.getBody().get("items");
    assertThat(items).isNotEmpty();
    assertThat(items.get(0).get("merchantCategory")).isEqualTo("GROCERY");
    assertThat(((Number) items.get(0).get("amount")).doubleValue()).isGreaterThan(0.0);
  }

  @Test
  void detail_returnsExplainabilityPayload() {
    ResponseEntity<Map> page =
        restTemplate.getForEntity(baseUrl() + "/audit/decisions?page=0&size=1", Map.class);
    assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<Map<String, Object>> items = (List<Map<String, Object>>) page.getBody().get("items");
    assertThat(items).isNotEmpty();
    String assessmentId = String.valueOf(items.get(0).get("assessmentId"));

    ResponseEntity<Map> response =
        restTemplate.getForEntity(baseUrl() + "/audit/decisions/" + assessmentId, Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("scoreBreakdown")).isNotNull();
    assertThat(response.getBody().get("reasonCodes")).isNotNull();
    assertThat(response.getBody().get("eventSnapshot")).isNotNull();
    assertThat(response.getBody().get("modelVersion")).isNotNull();
    assertThat(response.getBody().get("finalDecisionReason")).isNotNull();
  }

  @Test
  void trends_returnsHourlyBuckets() {
    ResponseEntity<List> response =
        restTemplate.getForEntity(baseUrl() + "/audit/decisions/trends?hours=24", List.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
  }

  @Test
  void scoreDistribution_returnsBuckets() {
    ResponseEntity<List> response =
        restTemplate.getForEntity(baseUrl() + "/audit/decisions/score-distribution", List.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody()).isNotEmpty();
  }

  @Test
  void reasonLeaderboard_returnsRankedReasons() {
    ResponseEntity<List> response =
        restTemplate.getForEntity(baseUrl() + "/audit/decisions/reason-leaderboard", List.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
  }
}
