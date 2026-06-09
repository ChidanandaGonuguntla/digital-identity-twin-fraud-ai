package com.citizens.digital.twin.infrastructure.security;

public record JwtTokenClaims(String email, String role) {}
