package com.citizens.digital.twin.infrastructure.security;

import com.citizens.digital.twin.infrastructure.config.CorsProperties;
import com.citizens.digital.twin.infrastructure.config.IdentityProperties;
import com.citizens.digital.twin.infrastructure.config.SecurityProperties;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      SecurityProperties securityProperties,
      IdentityProperties identityProperties,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      OidcJwtAuthenticationConverter oidcJwtAuthenticationConverter,
      JwtAuthenticationEntryPoint authenticationEntryPoint,
      JwtAccessDeniedHandler accessDeniedHandler)
      throws Exception {
    http.csrf(csrf -> csrf.disable());
    http.cors(cors -> {});
    http.sessionManagement(
        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    if (securityProperties.enabled()) {
      http.exceptionHandling(
          exceptions ->
              exceptions
                  .authenticationEntryPoint(authenticationEntryPoint)
                  .accessDeniedHandler(accessDeniedHandler));
      http.authorizeHttpRequests(
          auth ->
              auth.requestMatchers("/api/v1/auth/**")
                  .permitAll()
                  .requestMatchers("/actuator/health", "/actuator/health/**")
                  .permitAll()
                  .requestMatchers("/actuator/prometheus")
                  .permitAll()
                  .requestMatchers(HttpMethod.OPTIONS, "/**")
                  .permitAll()
                  .requestMatchers("/ws/**")
                  .permitAll()
                  .anyRequest()
                  .authenticated());
      if (identityProperties.oidcEnabled()) {
        http.oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(oidcJwtAuthenticationConverter)));
      } else {
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
      }
    } else {
      http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    }
    return http.build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(corsProperties.originList());
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
