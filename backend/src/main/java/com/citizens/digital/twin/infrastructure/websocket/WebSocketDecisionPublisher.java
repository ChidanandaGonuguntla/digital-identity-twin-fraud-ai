package com.citizens.digital.twin.infrastructure.websocket;

import com.citizens.digital.twin.api.dto.DecisionEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketDecisionPublisher {

  private final SimpMessagingTemplate template;

  public WebSocketDecisionPublisher(SimpMessagingTemplate template) {
    this.template = template;
  }

  public void publish(DecisionEvent event) {
    template.convertAndSend("/topic/decisions", event);
  }
}
