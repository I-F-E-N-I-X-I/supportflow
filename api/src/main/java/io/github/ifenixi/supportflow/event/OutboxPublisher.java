package io.github.ifenixi.supportflow.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.*;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxPublisher {
  private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
  private final JdbcTemplate jdbc;
  private final RabbitTemplate rabbit;
  private final ObjectMapper json;

  public OutboxPublisher(JdbcTemplate jdbc, RabbitTemplate rabbit, ObjectMapper json) {
    this.jdbc = jdbc;
    this.rabbit = rabbit;
    this.json = json;
  }

  @Transactional
  public int publishBatch() {
    var pending =
        jdbc.query(
            "SELECT * FROM outbox_event WHERE published_at IS NULL AND"
                + " next_attempt_at<=CURRENT_TIMESTAMP ORDER BY occurred_at,id LIMIT 10 FOR UPDATE"
                + " SKIP LOCKED",
            (r, n) ->
                new Pending(
                    new StatusEvent(
                        r.getObject("id", UUID.class),
                        r.getLong("ticket_id"),
                        r.getLong("client_id"),
                        r.getString("status"),
                        r.getTimestamp("occurred_at").toInstant()),
                    r.getInt("attempts")));
    int published = 0;
    for (var row : pending) {
      var event = row.event();
      try {
        var correlation = new CorrelationData(event.eventId().toString());
        var properties = new MessageProperties();
        properties.setContentType("application/json");
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setMessageId(event.eventId().toString());
        rabbit.send(
            BrokerConfig.EXCHANGE,
            "ticket.status",
            new Message(json.writeValueAsBytes(event), properties),
            correlation);
        var confirm = correlation.getFuture().get(3, TimeUnit.SECONDS);
        if (!confirm.isAck() || correlation.getReturned() != null)
          throw new IllegalStateException("Broker did not confirm routing");
        jdbc.update(
            "UPDATE outbox_event SET published_at=CURRENT_TIMESTAMP WHERE id=?", event.eventId());
        published++;
        log.info("Event published eventId={}", event.eventId());
      } catch (Exception e) {
        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        int attempts = Math.min(row.attempts() + 1, 1000000);
        long delay = Math.min(300L, 1L << Math.min(attempts, 8));
        jdbc.update(
            "UPDATE outbox_event SET attempts=?,next_attempt_at=? WHERE id=?",
            attempts,
            Timestamp.from(Instant.now().plusSeconds(delay)),
            event.eventId());
        log.warn("Event publish deferred eventId={} attempt={}", event.eventId(), attempts);
      }
    }
    return published;
  }

  private record Pending(StatusEvent event, int attempts) {}
}
