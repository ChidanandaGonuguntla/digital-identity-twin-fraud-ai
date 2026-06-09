package com.citizens.digital.twin.infrastructure.security;

import com.citizens.digital.twin.infrastructure.config.SecurityProperties;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {
  private final SecurityProperties securityProperties;
  private final AccessTokenValidator accessTokenValidator;

  public JwtHandshakeInterceptor(
      SecurityProperties securityProperties, AccessTokenValidator accessTokenValidator) {
    this.securityProperties = securityProperties;
    this.accessTokenValidator = accessTokenValidator;
  }

  @Override
  public boolean beforeHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes) {
    if (!securityProperties.enabled()) {
      return true;
    }
    if (!(request instanceof ServletServerHttpRequest servletRequest)) {
      return false;
    }
    String token = servletRequest.getServletRequest().getParameter("token");
    return accessTokenValidator.isValid(token);
  }

  @Override
  public void afterHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Exception exception) {}
}
