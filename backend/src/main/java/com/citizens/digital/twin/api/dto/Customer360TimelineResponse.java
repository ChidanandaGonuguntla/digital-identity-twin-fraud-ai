package com.citizens.digital.twin.api.dto;

import java.util.List;

public record Customer360TimelineResponse(
    String customerId,
    TwinExplorerResponse profile,
    CustomerVelocityResponse velocity,
    List<Customer360TimelineEvent> events) {}
