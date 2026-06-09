package com.citizens.digital.twin.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
public class RedisConfig {

  @Bean
  LettuceConnectionFactory redisConnectionFactory(RedisProperties properties) {
    RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
    configuration.setHostName(properties.host());
    configuration.setPort(properties.port());
    if (properties.password() != null && !properties.password().isBlank()) {
      configuration.setPassword(RedisPassword.of(properties.password()));
    }
    return new LettuceConnectionFactory(configuration);
  }

  @Bean
  StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory connectionFactory) {
    return new StringRedisTemplate(connectionFactory);
  }
}
