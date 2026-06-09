package com.citizens.digital.twin.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
@Conditional(OnOidcEnabledCondition.class)
public class OidcSecurityConfiguration {

  @Bean
  JwtDecoder jwtDecoder(IdentityProperties properties) {
    if (properties.jwksUri() != null && !properties.jwksUri().isBlank()) {
      return NimbusJwtDecoder.withJwkSetUri(properties.jwksUri()).build();
    }
    return JwtDecoders.fromIssuerLocation(properties.issuerUri());
  }
}
