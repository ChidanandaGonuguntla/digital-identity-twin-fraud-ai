package com.citizens.digital.twin.api.dto;

import java.util.List;

public record StepUpChallengePageResponse(
    List<StepUpChallengeItemResponse> items,
    int page,
    int size,
    long totalElements,
    int totalPages) {}
