package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.api.dto.DecisionEvent;

public interface DecisionPublisher {
  void publish(DecisionEvent event);
}
