package com.citizens.digital.twin.infrastructure.kafka;

import com.citizens.digital.twin.api.dto.FraudDecisionRequest;
import com.citizens.digital.twin.application.service.FraudDecisionApplicationService;
import com.citizens.digital.twin.infrastructure.observability.KafkaHeadersPropagator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "fraud.kafka", name = "enabled", havingValue = "true")
public class TransactionEventKafkaConsumer {
  private static final Logger log = LoggerFactory.getLogger(TransactionEventKafkaConsumer.class);
  private final FraudDecisionApplicationService service;

  public TransactionEventKafkaConsumer(FraudDecisionApplicationService service) {
    this.service = service;
  }

  @KafkaListener(
      topics = "${fraud.kafka.transaction-topic}",
      groupId = "${spring.kafka.consumer.group-id}",
      containerFactory = "fraudDecisionKafkaListenerContainerFactory")
  public void consume(ConsumerRecord<String, FraudDecisionRequest> record) {
    KafkaHeadersPropagator.apply(record.headers());
    try {
      FraudDecisionRequest request = record.value();
      log.info(
          "Kafka transaction received topic={} partition={} offset={} transactionId={}",
          record.topic(),
          record.partition(),
          record.offset(),
          request.transactionId());
      service.evaluate(request);
    } finally {
      KafkaHeadersPropagator.clear();
    }
  }
}
