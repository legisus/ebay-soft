-- Transactional outbox for ebay-conn-api event publishing. Same shape every service uses;
-- canonical DDL lives in libs/common-events/OutboxDdl#CREATE_OUTBOX.

CREATE TABLE IF NOT EXISTS outbox (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        TEXT NOT NULL UNIQUE,
    type            TEXT NOT NULL,
    source          TEXT NOT NULL,
    subject         TEXT,
    time            TIMESTAMPTZ NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    forwarded_at    TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS outbox_unforwarded_idx
    ON outbox (created_at)
    WHERE forwarded_at IS NULL;
