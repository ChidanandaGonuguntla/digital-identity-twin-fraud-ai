package com.citizens.digital.twin.api.dto;

public record FraudCaseStatusRequest(String status, String closureReason, String notes) {}
