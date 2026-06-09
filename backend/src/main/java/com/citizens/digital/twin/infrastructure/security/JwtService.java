package com.citizens.digital.twin.infrastructure.security;

import com.citizens.digital.twin.infrastructure.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final SecurityProperties properties;
  private final SecretKey secretKey;

  public JwtService(SecurityProperties properties) {
    this.properties = properties;
    this.secretKey = Keys.hmacShaKeyFor(properties.jwtSecret().getBytes(StandardCharsets.UTF_8));
  }

  public String createToken(String email, String role) {
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(properties.jwtExpirationMinutes() * 60L);
    return Jwts.builder()
        .subject(email)
        .claim("role", role)
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiresAt))
        .signWith(secretKey)
        .compact();
  }

  public Optional<JwtTokenClaims> parse(String token) {
    if (token == null || token.isBlank()) {
      return Optional.empty();
    }
    try {
      Claims claims =
          Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
      String role = claims.get("role", String.class);
      return Optional.of(new JwtTokenClaims(claims.getSubject(), role));
    } catch (RuntimeException ex) {
      return Optional.empty();
    }
  }

  public long expirationSeconds() {
    return properties.jwtExpirationMinutes() * 60L;
  }
}
