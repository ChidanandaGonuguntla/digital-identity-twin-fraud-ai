package com.citizens.digital.twin.infrastructure.observability;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

@Component("kafka")
@ConditionalOnProperty(prefix = "fraud.kafka", name = "enabled", havingValue = "true")
public class KafkaHealthIndicator implements HealthIndicator {
  private final KafkaAdmin kafkaAdmin;

  public KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
    this.kafkaAdmin = kafkaAdmin;
  }

  @Override
  public Health health() {
    Properties properties = new Properties();
    properties.putAll(kafkaAdmin.getConfigurationProperties());
    try (AdminClient adminClient = AdminClient.create(properties)) {
      adminClient.describeCluster().nodes().get(3, TimeUnit.SECONDS);
      return Health.up().withDetail("status", "reachable").build();
    } catch (Exception ex) {
      return Health.down(ex).withDetail("status", "unreachable").build();
    }
  }
}
