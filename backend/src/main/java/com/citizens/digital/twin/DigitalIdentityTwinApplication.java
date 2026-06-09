package com.citizens.digital.twin;

import com.citizens.digital.twin.infrastructure.config.CorsProperties;
import com.citizens.digital.twin.infrastructure.config.IdentityProperties;
import com.citizens.digital.twin.infrastructure.config.KafkaTopicProperties;
import com.citizens.digital.twin.infrastructure.config.MlModelProperties;
import com.citizens.digital.twin.infrastructure.config.RedisProperties;
import com.citizens.digital.twin.infrastructure.config.ScoringProperties;
import com.citizens.digital.twin.infrastructure.config.SecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
  ScoringProperties.class,
  KafkaTopicProperties.class,
  CorsProperties.class,
  SecurityProperties.class,
  IdentityProperties.class,
  RedisProperties.class,
  MlModelProperties.class
})
public class DigitalIdentityTwinApplication {
  public static void main(String[] args) {
    SpringApplication.run(DigitalIdentityTwinApplication.class, args);
  }
}
