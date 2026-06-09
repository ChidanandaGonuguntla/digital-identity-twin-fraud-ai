package com.citizens.digital.twin.api.dto;

import java.time.Instant;

public record AuditTrendPointResponse(Instant bucket, long allow, long review, long block) {}
