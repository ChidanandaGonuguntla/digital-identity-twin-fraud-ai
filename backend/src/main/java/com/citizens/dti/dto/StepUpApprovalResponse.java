package com.citizens.dti.dto;

import java.util.UUID;

public record StepUpApprovalResponse(
    UUID challengeId, String challengeStatus, boolean transactionAllowed, String message) {}
