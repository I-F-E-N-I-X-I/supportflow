package io.github.ifenixi.supportflow.event;

import org.slf4j.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.outbox-enabled", havingValue = "true", matchIfMissing = true)
public class OutboxScheduler {
  private static final Logger log = LoggerFactory.getLogger(OutboxScheduler.class);
  private final OutboxPublisher publisher;

  public OutboxScheduler(OutboxPublisher publisher) {
    this.publisher = publisher;
  }

  @Scheduled(fixedDelayString = "${app.outbox-delay-ms:1000}")
  public void publish() {
    try {
      publisher.publishBatch();
    } catch (Exception e) {
      log.error("Outbox batch failed type={}", e.getClass().getSimpleName());
    }
  }
}
