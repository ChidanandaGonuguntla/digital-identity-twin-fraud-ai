package com.citizens.digital.twin.infrastructure.observability;

import com.citizens.digital.twin.infrastructure.config.KafkaTopicProperties;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsSpec;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "fraud.kafka", name = "enabled", havingValue = "true")
public class KafkaConsumerLagMonitor {
  private static final Logger log = LoggerFactory.getLogger(KafkaConsumerLagMonitor.class);

  private final KafkaAdmin kafkaAdmin;
  private final KafkaTopicProperties topicProperties;
  private final FraudMetricsService fraudMetricsService;
  private final String consumerGroupId;

  public KafkaConsumerLagMonitor(
      KafkaAdmin kafkaAdmin,
      KafkaTopicProperties topicProperties,
      FraudMetricsService fraudMetricsService,
      @Value("${spring.kafka.consumer.group-id}") String consumerGroupId) {
    this.kafkaAdmin = kafkaAdmin;
    this.topicProperties = topicProperties;
    this.fraudMetricsService = fraudMetricsService;
    this.consumerGroupId = consumerGroupId;
  }

  @Scheduled(fixedDelayString = "${fraud.kafka.lag-monitor-interval-ms:30000}")
  public void pollLag() {
    Properties properties = new Properties();
    properties.putAll(kafkaAdmin.getConfigurationProperties());
    try (AdminClient adminClient = AdminClient.create(properties)) {
      Map<String, Map<TopicPartition, OffsetAndMetadata>> committed =
          adminClient
              .listConsumerGroupOffsets(
                  Collections.singletonMap(consumerGroupId, new ListConsumerGroupOffsetsSpec()))
              .all()
              .get(5, TimeUnit.SECONDS);
      Map<TopicPartition, OffsetAndMetadata> partitions = committed.get(consumerGroupId);
      if (partitions == null || partitions.isEmpty()) {
        fraudMetricsService.updateKafkaLag(0);
        return;
      }
      AtomicLong totalLag = new AtomicLong(0);
      for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : partitions.entrySet()) {
        TopicPartition partition = entry.getKey();
        if (!topicProperties.transactionTopic().equals(partition.topic())) {
          continue;
        }
        ListOffsetsResult.ListOffsetsResultInfo end =
            adminClient
                .listOffsets(Collections.singletonMap(partition, OffsetSpec.latest()))
                .partitionResult(partition)
                .get(5, TimeUnit.SECONDS);
        long lag = Math.max(0, end.offset() - entry.getValue().offset());
        totalLag.addAndGet(lag);
      }
      fraudMetricsService.updateKafkaLag(totalLag.get());
    } catch (Exception ex) {
      log.warn("Kafka lag monitor failed", ex);
    }
  }
}
