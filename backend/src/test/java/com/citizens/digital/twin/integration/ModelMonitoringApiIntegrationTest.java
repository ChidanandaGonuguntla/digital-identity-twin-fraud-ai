package com.citizens.digital.twin.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ModelMonitoringApiIntegrationTest extends AbstractPostgresIntegrationTest {

  @BeforeEach
  void seedAuditRow() {
    postDecision("CUST-MODEL-001", "TXN-MODEL-001");
  }

  @Test
  void metadata_returnsModelIdentity() {
    ResponseEntity<Map> response =
        restTemplate.getForEntity(baseUrl() + "/models/fraud-risk/metadata", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("modelName")).isNotNull();
    assertThat(response.getBody().get("modelVersion")).isNotNull();
  }

  @Test
  void health_returnsOperationalSnapshot() {
    ResponseEntity<Map> response =
        restTemplate.getForEntity(baseUrl() + "/models/fraud-risk/health", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("status")).isNotNull();
  }

  @Test
  void liveMetrics_returnsRealtimeCounters() {
    ResponseEntity<Map> response =
        restTemplate.getForEntity(baseUrl() + "/models/fraud-risk/live-metrics", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("scoredLastHour")).isNotNull();
  }

  @Test
  void driftTrend_returnsTimeSeries() {
    ResponseEntity<List> response =
        restTemplate.getForEntity(
            baseUrl() + "/models/fraud-risk/drift-trend?hours=24", List.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
  }

  @Test
  void qualityMetrics_returnsClassificationMetrics() {
    ResponseEntity<Map> response =
        restTemplate.getForEntity(baseUrl() + "/models/fraud-risk/quality-metrics", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("precision")).isNotNull();
    assertThat(response.getBody().get("recall")).isNotNull();
    assertThat(response.getBody().get("auc")).isNotNull();
  }
}
