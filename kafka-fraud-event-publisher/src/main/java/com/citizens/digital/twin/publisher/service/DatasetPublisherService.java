package com.citizens.digital.twin.publisher.service;

import com.citizens.digital.twin.publisher.PublisherProperties;
import com.citizens.digital.twin.publisher.model.TransactionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class DatasetPublisherService {
  private static final Logger log = LoggerFactory.getLogger(DatasetPublisherService.class);
  private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;
  private final ObjectMapper objectMapper;
  private final PublisherProperties props;

  public DatasetPublisherService(
      KafkaTemplate<String, TransactionEvent> kafkaTemplate,
      ObjectMapper objectMapper,
      PublisherProperties props) {
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = objectMapper;
    this.props = props;
  }

  public int publishDataset() throws Exception {
    Path path = Path.of(props.datasetPath());
    if (!Files.exists(path) && !path.isAbsolute()) {
      Path modulePath = Path.of("kafka-fraud-event-publisher", props.datasetPath());
      if (Files.exists(modulePath)) {
        path = modulePath;
      }
    }
    if (!Files.exists(path)) {
      throw new IllegalArgumentException("Dataset not found: " + path.toAbsolutePath());
    }

    int rate = Math.max(1, props.publishRatePerSecond());
    long sleepEvery = Math.max(1, rate);
    AtomicInteger count = new AtomicInteger();
    Instant start = Instant.now();

    try (BufferedReader reader = Files.newBufferedReader(path)) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (props.maxRecords() > 0 && count.get() >= props.maxRecords()) {
          break;
        }
        TransactionEvent event = objectMapper.readValue(line, TransactionEvent.class);
        kafkaTemplate.send(props.topic(), event.customerId(), event);
        int current = count.incrementAndGet();
        if (current % 1000 == 0) {
          log.info("Published {} events to topic {}", current, props.topic());
        }
        if (current % sleepEvery == 0) {
          Thread.sleep(1000L);
        }
      }
    }
    kafkaTemplate.flush();
    log.info(
        "Completed publishing {} events in {} seconds",
        count.get(),
        Duration.between(start, Instant.now()).toSeconds());
    return count.get();
  }
}
