package io.github.ifenixi.supportflow.event;

import java.time.Instant;
import java.util.UUID;

public record StatusEvent(
    UUID eventId, long ticketId, long clientId, String status, Instant occurredAt) {}
