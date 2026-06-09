package com.citizens.digital.twin.api.dto;

import java.time.Instant;

public record StepUpChallengeResponse(
    String challengeId, String status, String message, Instant createdAt) {}
