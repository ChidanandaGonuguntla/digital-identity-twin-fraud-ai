package com.citizens.dti.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StepUpChallengeResponse(
    UUID challengeId,
    String challengeType,
    String challengeStatus,
    String deliveryChannel,
    String destinationLabel,
    OffsetDateTime expiresAt,
    String reasonCode) {}
