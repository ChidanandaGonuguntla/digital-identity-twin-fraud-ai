package com.citizens.dti.web;

import com.citizens.dti.model.RiskAssessment;
import com.citizens.dti.model.TransactionEvent;
import org.springframework.stereotype.Service;

/**
 * Thin seam between the fraud domain and the live stream. The domain service calls {@link #publish}
 * after each assessment; the publisher builds the wire event and hands it to the WebSocket handler.
 * Keeping this separate means the scoring logic has no dependency on the transport.
 */
@Service
public class DecisionPublisher {

  private final DecisionStreamHandler handler;
  private final DecisionEventFactory factory;

  public DecisionPublisher(DecisionStreamHandler handler, DecisionEventFactory factory) {
    this.handler = handler;
    this.factory = factory;
  }

  public void publish(TransactionEvent event, RiskAssessment assessment) {
    handler.broadcast(factory.from(event, assessment));
  }
}
