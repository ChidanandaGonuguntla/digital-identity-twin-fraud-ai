package com.citizens.dti.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;


@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  private final DecisionStreamHandler handler;

  public WebSocketConfig(DecisionStreamHandler handler) {
    this.handler = handler;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry
        .addHandler(handler, "/ws/decisions")
        .setAllowedOriginPatterns("*")
        .setAllowedOrigins(
            "http://localhost:5173", "http://localhost:5174", "http://localhost:3000");
    ;
  }
}
