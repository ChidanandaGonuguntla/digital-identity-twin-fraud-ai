package com.citizens.digital.twin.api.dto;

import java.util.List;

public record FraudCasePageResponse(
    List<FraudCaseSummaryResponse> items, int page, int size, long totalElements, int totalPages) {}
