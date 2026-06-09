package com.citizens.digital.twin.api.dto;

public record AuthConfigResponse(
    boolean securityEnabled, String provider, String issuerUri, String audience, String jwksUri) {}
