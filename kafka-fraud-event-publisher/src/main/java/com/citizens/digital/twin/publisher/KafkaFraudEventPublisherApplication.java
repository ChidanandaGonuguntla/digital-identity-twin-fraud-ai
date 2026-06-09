package com.citizens.digital.twin.publisher;

import com.citizens.digital.twin.publisher.service.DatasetPublisherService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(PublisherProperties.class)
public class KafkaFraudEventPublisherApplication {
  public static void main(String[] args) {
    SpringApplication.run(KafkaFraudEventPublisherApplication.class, args);
  }

  @Bean
  CommandLineRunner publishOnStartup(PublisherProperties props, DatasetPublisherService service) {
    return args -> {
      if (props.publishOnStartup()) {
        service.publishDataset();
      }
    };
  }
}
