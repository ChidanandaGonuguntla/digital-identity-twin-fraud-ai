package com.citizens.digital.twin.infrastructure.kafka;

import com.citizens.digital.twin.api.dto.DecisionEvent;
import com.citizens.digital.twin.application.service.DecisionPublisher;
import com.citizens.digital.twin.infrastructure.websocket.WebSocketDecisionPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class CompositeDecisionPublisher implements DecisionPublisher {

  private final WebSocketDecisionPublisher webSocketPublisher;

  private final ObjectProvider<FraudKafkaEventPublisher> kafkaPublisher;

  public CompositeDecisionPublisher(
      WebSocketDecisionPublisher webSocketPublisher,
      ObjectProvider<FraudKafkaEventPublisher> kafkaPublisher) {
    this.webSocketPublisher = webSocketPublisher;
    this.kafkaPublisher = kafkaPublisher;
  }

  @Override
  public void publish(DecisionEvent event) {
    webSocketPublisher.publish(event);
    kafkaPublisher.ifAvailable(publisher -> publisher.publishDecision(event));
  }
}
