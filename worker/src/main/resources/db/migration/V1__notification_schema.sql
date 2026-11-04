CREATE TABLE notification (
 event_id UUID PRIMARY KEY,
 ticket_id BIGINT NOT NULL,
 client_id BIGINT NOT NULL,
 status VARCHAR(16) NOT NULL,
 occurred_at TIMESTAMPTZ NOT NULL,
 received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_notification_client ON notification(client_id,occurred_at DESC);
