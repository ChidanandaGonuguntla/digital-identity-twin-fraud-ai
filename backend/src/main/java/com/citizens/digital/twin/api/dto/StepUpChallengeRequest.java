package com.citizens.digital.twin.api.dto;

public record StepUpChallengeRequest(
    String assessmentId, String customerId, String transactionId, String channel, String reason) {}
