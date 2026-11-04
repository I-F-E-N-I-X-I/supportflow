package io.github.ifenixi.supportflow.event;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.*;
@Component @ConditionalOnProperty(name="app.outbox-enabled",havingValue="true",matchIfMissing=true)
public class OutboxScheduler {
 private static final Logger log=LoggerFactory.getLogger(OutboxScheduler.class);
 private final OutboxPublisher publisher;
 public OutboxScheduler(OutboxPublisher publisher){this.publisher=publisher;}
 @Scheduled(fixedDelayString="${app.outbox-delay-ms:1000}") public void publish(){
  try{publisher.publishBatch();}catch(Exception e){log.error("Outbox batch failed type={}",e.getClass().getSimpleName());}
 }
}
