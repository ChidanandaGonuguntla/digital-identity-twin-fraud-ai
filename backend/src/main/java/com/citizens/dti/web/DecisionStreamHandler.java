package com.citizens.dti.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class DecisionStreamHandler extends TextWebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(DecisionStreamHandler.class);
  private static final int SEND_TIME_LIMIT_MS = 10_000;
  private static final int BUFFER_SIZE_LIMIT = 512 * 1024;

  private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
  private final ObjectMapper mapper;

  public DecisionStreamHandler(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    sessions.add(
        new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT));
    log.info("Decision stream connected: {} ({} clients)", session.getId(), sessions.size());
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    sessions.removeIf(s -> s.getId().equals(session.getId()));
    log.info("Decision stream disconnected: {} ({} clients)", session.getId(), sessions.size());
  }

  /** Broadcast a decided transaction to every connected console. Non-blocking-ish, best-effort. */
  public void broadcast(DecisionEvent event) {
    if (sessions.isEmpty()) {
      return;
    }
    final String json;
    try {
      json = mapper.writeValueAsString(event);
    } catch (JsonProcessingException ex) {
      log.warn("Failed to serialize DecisionEvent {}", event.transactionId(), ex);
      return;
    }
    TextMessage message = new TextMessage(json);
    for (WebSocketSession session : sessions) {
      try {
        if (session.isOpen()) {
          session.sendMessage(message);
        } else {
          sessions.remove(session);
        }
      } catch (IOException ex) {
        log.debug("Send failed; dropping session {}", session.getId());
        sessions.remove(session);
      }
    }
  }

  public int clientCount() {
    return sessions.size();
  }
}
