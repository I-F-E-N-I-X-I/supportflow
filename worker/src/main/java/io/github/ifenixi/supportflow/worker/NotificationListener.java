package io.github.ifenixi.supportflow.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.*;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {
  private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);
  private final NotificationStore store;
  private final ObjectMapper json;

  public NotificationListener(NotificationStore store, ObjectMapper json) {
    this.store = store;
    this.json = json;
  }

  @RabbitListener(queues = BrokerConfig.QUEUE)
  public void receive(Message message) throws Exception {
    try {
      var event = json.readValue(message.getBody(), StatusEvent.class);
      store.accept(event);
      log.info("Notification persisted eventId={}", event.eventId());
    } catch (Exception e) {
      log.warn("Notification processing failed type={}", e.getClass().getSimpleName());
      throw e;
    }
  }
}
