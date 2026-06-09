package com.citizens.digital.twin.infrastructure.config;

import com.citizens.digital.twin.infrastructure.security.JwtHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  private final CorsProperties corsProperties;
  private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

  public WebSocketConfig(
      CorsProperties corsProperties, JwtHandshakeInterceptor jwtHandshakeInterceptor) {
    this.corsProperties = corsProperties;
    this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic");
    registry.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    String[] origins = corsProperties.originList().toArray(String[]::new);
    registry
        .addEndpoint("/ws/decisions")
        .setAllowedOrigins(origins)
        .addInterceptors(jwtHandshakeInterceptor);
  }
}
