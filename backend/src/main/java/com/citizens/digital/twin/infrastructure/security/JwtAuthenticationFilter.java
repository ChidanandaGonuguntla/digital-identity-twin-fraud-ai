package com.citizens.digital.twin.infrastructure.security;

import com.citizens.digital.twin.infrastructure.config.IdentityProperties;
import com.citizens.digital.twin.infrastructure.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final SecurityProperties securityProperties;
  private final IdentityProperties identityProperties;
  private final JwtService jwtService;

  public JwtAuthenticationFilter(
      SecurityProperties securityProperties,
      IdentityProperties identityProperties,
      JwtService jwtService) {
    this.securityProperties = securityProperties;
    this.identityProperties = identityProperties;
    this.jwtService = jwtService;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !securityProperties.enabled() || identityProperties.oidcEnabled();
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null && header.startsWith("Bearer ")) {
      jwtService
          .parse(header.substring(7))
          .ifPresent(
              claims -> {
                var authentication =
                    new UsernamePasswordAuthenticationToken(
                        claims.email(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + claims.role())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
              });
    }
    filterChain.doFilter(request, response);
  }
}
