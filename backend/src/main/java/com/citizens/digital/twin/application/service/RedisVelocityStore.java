package com.citizens.digital.twin.application.service;

import com.citizens.digital.twin.domain.model.Decision;
import com.citizens.digital.twin.infrastructure.config.RedisProperties;
import java.time.Duration;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
public class RedisVelocityStore {
  private static final Duration TTL_5M = Duration.ofMinutes(5);
  private static final Duration TTL_1H = Duration.ofHours(1);
  private static final Duration TTL_24H = Duration.ofHours(24);
  private static final Duration TTL_30M = Duration.ofMinutes(30);

  private final StringRedisTemplate redis;
  private final RedisProperties properties;

  public RedisVelocityStore(StringRedisTemplate redis, RedisProperties properties) {
    this.redis = redis;
    this.properties = properties;
  }

  public boolean available() {
    return properties.enabled();
  }

  public void record(String customerId, double amount, Decision decision) {
    increment(key(customerId, "txn:5m"), TTL_5M);
    increment(key(customerId, "txn:1h"), TTL_1H);
    increment(key(customerId, "txn:24h"), TTL_24H);
    incrementAmount(key(customerId, "amount:1h"), amount, TTL_1H);
    if (decision == Decision.BLOCK || decision == Decision.CHALLENGE) {
      increment(key(customerId, "failed:30m"), TTL_30M);
    }
  }

  public Optional<RedisVelocitySnapshot> snapshot(String customerId) {
    return Optional.of(
        new RedisVelocitySnapshot(
            parseInt(key(customerId, "txn:5m")),
            parseInt(key(customerId, "txn:1h")),
            parseInt(key(customerId, "txn:24h")),
            parseDouble(key(customerId, "amount:1h")),
            parseInt(key(customerId, "failed:30m"))));
  }

  private void increment(String redisKey, Duration ttl) {
    Long value = redis.opsForValue().increment(redisKey);
    if (value != null && value == 1L) {
      redis.expire(redisKey, ttl);
    }
  }

  private void incrementAmount(String redisKey, double amount, Duration ttl) {
    Double value = redis.opsForValue().increment(redisKey, amount);
    if (value != null && Math.abs(value - amount) < 0.000001d) {
      redis.expire(redisKey, ttl);
    }
  }

  private int parseInt(String redisKey) {
    String value = redis.opsForValue().get(redisKey);
    if (value == null || value.isBlank()) {
      return 0;
    }
    return (int) Math.round(Double.parseDouble(value));
  }

  private double parseDouble(String redisKey) {
    String value = redis.opsForValue().get(redisKey);
    if (value == null || value.isBlank()) {
      return 0.0;
    }
    return Double.parseDouble(value);
  }

  private String key(String customerId, String suffix) {
    return "dti:velocity:" + customerId + ":" + suffix;
  }

  public record RedisVelocitySnapshot(
      int txnCount5m, int txnCount1h, int txnCount24h, double amountSum1h, int failedAttempts30m) {}
}
