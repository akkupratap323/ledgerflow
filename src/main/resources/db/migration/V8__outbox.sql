-- Transactional outbox: events are written in the same transaction as the ledger change,
-- then delivered asynchronously. No event is lost and none is sent for a rolled back write.
CREATE TABLE outbox_events (
    id              UUID         PRIMARY KEY,
    aggregate_type  VARCHAR(50)  NOT NULL,
    aggregate_id    UUID         NOT NULL,
    event_type      VARCHAR(50)  NOT NULL,
    payload         TEXT         NOT NULL,
    status          VARCHAR(10)  NOT NULL,
    attempts        INT          NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_error      VARCHAR(500),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    sent_at         TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_outbox_status CHECK (status IN ('PENDING', 'SENT', 'DEAD'))
);

CREATE INDEX ix_outbox_pending ON outbox_events (status, next_attempt_at);
