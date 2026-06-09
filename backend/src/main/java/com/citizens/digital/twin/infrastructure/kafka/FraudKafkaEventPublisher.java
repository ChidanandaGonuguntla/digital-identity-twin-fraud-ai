package com.citizens.digital.twin.infrastructure.kafka;

import com.citizens.digital.twin.api.dto.DecisionEvent;
import com.citizens.digital.twin.infrastructure.config.KafkaTopicProperties;
import com.citizens.digital.twin.infrastructure.kafka.event.FraudAuditEvent;
import com.citizens.digital.twin.infrastructure.kafka.event.StepUpChallengeEvent;
import com.citizens.digital.twin.infrastructure.kafka.event.TwinDriftEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "fraud.kafka", name = "enabled", havingValue = "true")
public class FraudKafkaEventPublisher {
  private static final Logger log = LoggerFactory.getLogger(FraudKafkaEventPublisher.class);

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final KafkaTopicProperties props;

  public FraudKafkaEventPublisher(
      KafkaTemplate<String, Object> kafkaTemplate, KafkaTopicProperties props) {
    this.kafkaTemplate = kafkaTemplate;
    this.props = props;
  }

  public void publishDecision(DecisionEvent event) {
    send(props.decisionTopic(), event.transactionId(), event, props.decisionDltTopic());
  }

  public void publishAudit(FraudAuditEvent event) {
    kafkaTemplate.send(props.auditTopic(), event.assessmentId(), event);
  }

  public void publishStepUp(StepUpChallengeEvent event) {
    kafkaTemplate.send(props.stepUpTopic(), event.challengeId(), event);
  }

  public void publishDrift(TwinDriftEvent event) {
    kafkaTemplate.send(props.twinDriftTopic(), event.customerId(), event);
  }

  private void send(String topic, String key, Object payload, String dltTopic) {
    kafkaTemplate
        .send(topic, key, payload)
        .whenComplete(
            (result, ex) -> {
              if (ex != null) {
                log.error("Kafka publish failed topic={} key={}", topic, key, ex);
                kafkaTemplate.send(dltTopic, key, payload);
              }
            });
  }
}
