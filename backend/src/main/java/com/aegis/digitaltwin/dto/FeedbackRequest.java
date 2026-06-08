package com.aegis.digitaltwin.dto;

import com.aegis.digitaltwin.domain.FeedbackOutcome;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FeedbackRequest(
    @NotBlank String eventId,
    @NotBlank String customerId,
    @NotNull FeedbackOutcome outcome,
    String comments) {}
