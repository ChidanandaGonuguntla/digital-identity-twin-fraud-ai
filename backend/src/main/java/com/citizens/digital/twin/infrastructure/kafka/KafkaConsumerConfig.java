package com.citizens.digital.twin.infrastructure.kafka;

import com.citizens.digital.twin.api.dto.FraudDecisionRequest;
import com.citizens.digital.twin.infrastructure.config.KafkaTopicProperties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@ConditionalOnProperty(prefix = "fraud.kafka", name = "enabled", havingValue = "true")
public class KafkaConsumerConfig {

  @Bean
  ConcurrentKafkaListenerContainerFactory<String, FraudDecisionRequest>
      fraudDecisionKafkaListenerContainerFactory(
          ConsumerFactory<String, FraudDecisionRequest> consumerFactory,
          KafkaTemplate<String, Object> kafkaTemplate,
          KafkaTopicProperties props) {
    ConcurrentKafkaListenerContainerFactory<String, FraudDecisionRequest> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    factory.setConcurrency(2);
    DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (ConsumerRecord<?, ?> record, Exception ex) ->
                new TopicPartition(props.transactionDltTopic(), record.partition()));
    DefaultErrorHandler errorHandler =
        new DefaultErrorHandler(
            recoverer,
            new FixedBackOff(props.consumerRetryIntervalMs(), props.consumerRetryAttempts()));
    factory.setCommonErrorHandler(errorHandler);
    return factory;
  }
}
