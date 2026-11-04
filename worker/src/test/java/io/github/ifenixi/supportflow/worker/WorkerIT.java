package io.github.ifenixi.supportflow.worker;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.*;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.junit.jupiter.Container;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class WorkerIT {
  @Container static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:17-alpine");
  @Container static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.1-alpine");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", pg::getJdbcUrl);
    r.add("spring.datasource.username", pg::getUsername);
    r.add("spring.datasource.password", pg::getPassword);
    r.add("spring.rabbitmq.host", rabbit::getHost);
    r.add("spring.rabbitmq.port", rabbit::getAmqpPort);
    r.add("spring.rabbitmq.username", rabbit::getAdminUsername);
    r.add("spring.rabbitmq.password", rabbit::getAdminPassword);
    r.add("app.internal-token", () -> "test-only-internal-token-at-least-32-chars");
  }

  @Autowired NotificationStore store;
  @Autowired RabbitTemplate broker;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired MockMvc mvc;

  StatusEvent event() {
    return new StatusEvent(UUID.randomUUID(), 1, 123, "NEW", Instant.now());
  }

  @Test
  void duplicateEventIsPersistedOnce() {
    var e = event();
    store.accept(e);
    store.accept(e);
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM notification WHERE event_id=?", Integer.class, e.eventId()));
  }

  @Test
  void conflictingEventIdRejected() {
    var e = event();
    store.accept(e);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            store.accept(
                new StatusEvent(e.eventId(), 2, e.clientId(), e.status(), e.occurredAt())));
  }

  @Test
  void malformedEventRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> store.accept(new StatusEvent(null, 0, 0, "INVALID", null)));
  }

  @Test
  void rabbitDeliveryIsConsumedIdempotently() throws Exception {
    var e = event();
    for (int i = 0; i < 2; i++)
      broker.send(
          BrokerConfig.EXCHANGE,
          "ticket.status",
          new Message(json.writeValueAsBytes(e), new MessageProperties()));
    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () ->
                assertEquals(
                    1,
                    jdbc.queryForObject(
                        "SELECT COUNT(*) FROM notification WHERE event_id=?",
                        Integer.class,
                        e.eventId())));
  }

  @Test
  void poisonMessageGoesToDeadLetterQueue() {
    broker.send(
        BrokerConfig.EXCHANGE,
        "ticket.status",
        new Message(
            "invalid-json".getBytes(java.nio.charset.StandardCharsets.UTF_8),
            new MessageProperties()));
    await()
        .atMost(Duration.ofSeconds(20))
        .until(() -> broker.receive("supportflow.notifications.dlq") != null);
  }

  @Test
  void inboxIsScopedToClient() {
    var e = event();
    store.accept(e);
    assertTrue(store.inbox(999999, 0, 20).items().isEmpty());
  }

  @Test
  void internalEndpointRejectsAnonymous() throws Exception {
    mvc.perform(get("/internal/notifications?clientId=123")).andExpect(status().isUnauthorized());
  }

  @Test
  void authenticatedInternalRequestWorks() throws Exception {
    mvc.perform(
            get("/internal/notifications?clientId=123")
                .header("X-Internal-Token", "test-only-internal-token-at-least-32-chars"))
        .andExpect(status().isOk());
  }

  @Test
  void invalidPaginationRejected() throws Exception {
    mvc.perform(
            get("/internal/notifications?clientId=123&size=101")
                .header("X-Internal-Token", "test-only-internal-token-at-least-32-chars"))
        .andExpect(status().isBadRequest());
  }
}
