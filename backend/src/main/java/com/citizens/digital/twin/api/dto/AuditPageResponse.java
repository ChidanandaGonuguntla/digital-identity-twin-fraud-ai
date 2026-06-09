package com.citizens.digital.twin.api.dto;

import java.util.List;

public record AuditPageResponse(
    List<AuditDecisionResponse> items, int page, int size, long totalElements, int totalPages) {}
