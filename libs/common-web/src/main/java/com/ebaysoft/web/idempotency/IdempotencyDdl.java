package com.ebaysoft.web.idempotency;

/**
 * Canonical DDL fragment for the {@code idempotency_keys} table inside each service's schema.
 * Services paste this into their Flyway migration so every store sits on the same shape — see the
 * docs/API_DOCS.md "Idempotency" section + issue #161 for the contract.
 *
 * <p><b>Key shape:</b>
 *
 * <ul>
 *   <li>{@code (tenant_id, key)} is the primary key — keys are scoped per tenant so two tenants
 *       can both use {@code 00000000-0000-0000-0000-000000000001} without collision.
 *   <li>{@code request_hash} = SHA-256 of {@code method + "\n" + path + "\n" + normalized-body}.
 *       A retry with the same key + same hash returns the cached response. A retry with the same
 *       key + different hash returns 409.
 *   <li>{@code response_status} + {@code response_body} hold the served outcome of the first
 *       successful (2xx) attempt. 4xx/5xx outcomes don't poison the key — retrying after a real
 *       error is allowed.
 *   <li>{@code expires_at} runs 24h by default per the API_DOCS.md contract; a cleanup job
 *       periodically reaps expired rows. Index supports that scan.
 * </ul>
 */
public final class IdempotencyDdl {

  private IdempotencyDdl() {}

  public static final String CREATE_IDEMPOTENCY_KEYS =
      """
      CREATE TABLE IF NOT EXISTS idempotency_keys (
        tenant_id        UUID NOT NULL,
        key              TEXT NOT NULL,
        request_hash     BYTEA NOT NULL,
        response_status  INT NOT NULL,
        response_body    JSONB,
        created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
        expires_at       TIMESTAMPTZ NOT NULL,
        PRIMARY KEY (tenant_id, key)
      );

      CREATE INDEX IF NOT EXISTS idempotency_keys_expires_idx
        ON idempotency_keys (expires_at);
      """;
}
