package com.citizens.digital.twin.api.dto;

public record LoginResponse(
    String accessToken, String tokenType, long expiresIn, String email, String role) {}
