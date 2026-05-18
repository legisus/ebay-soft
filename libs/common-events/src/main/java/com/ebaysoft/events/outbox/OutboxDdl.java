package com.ebaysoft.events.outbox;

/**
 * Canonical DDL fragment for the transactional outbox table inside each service's schema. Services
 * paste this into their Flyway {@code V001__init.sql} (or later) so every outbox follows the same
 * shape — the forwarder relies on this contract.
 *
 * <p>One outbox per service; the forwarder reads its own outbox only and emits Postgres NOTIFY.
 */
public final class OutboxDdl {

  private OutboxDdl() {}

  public static final String CREATE_OUTBOX =
      """
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
      """;
}
