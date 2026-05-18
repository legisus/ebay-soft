-- sync-api owns three streams of eBay data: orders, listings, finance events. Each row carries
-- the tenant_id (denormalized) and the per-stream watermark needed to resume after a crash.
-- Schema is intentionally minimal — full normalization happens as the backfill matures.

CREATE TABLE sync_watermarks (
    tenant_id      UUID NOT NULL,
    stream         TEXT NOT NULL CHECK (stream IN ('orders', 'listings', 'finance')),
    last_seen_at   TIMESTAMPTZ NOT NULL,
    last_seen_id   TEXT,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (tenant_id, stream)
);

CREATE TABLE orders (
    id                 BIGSERIAL PRIMARY KEY,
    tenant_id          UUID NOT NULL,
    ebay_order_id      TEXT NOT NULL,
    placed_at          TIMESTAMPTZ NOT NULL,
    buyer_pseudonym    TEXT,
    currency           CHAR(3) NOT NULL,
    item_total         NUMERIC(12,2) NOT NULL,
    fees_total         NUMERIC(12,2) NOT NULL DEFAULT 0,
    shipping_total     NUMERIC(12,2) NOT NULL DEFAULT 0,
    tax_total          NUMERIC(12,2) NOT NULL DEFAULT 0,
    refunds_total      NUMERIC(12,2) NOT NULL DEFAULT 0,
    status             TEXT NOT NULL,
    raw                JSONB,
    UNIQUE (tenant_id, ebay_order_id)
);
CREATE INDEX orders_tenant_placed_idx ON orders (tenant_id, placed_at DESC);

CREATE TABLE listings (
    id                 BIGSERIAL PRIMARY KEY,
    tenant_id          UUID NOT NULL,
    ebay_item_id       TEXT NOT NULL,
    sku_code           TEXT,
    title              TEXT NOT NULL,
    category_id        TEXT,
    currency           CHAR(3) NOT NULL,
    current_price      NUMERIC(12,2) NOT NULL,
    status             TEXT NOT NULL,
    listed_at          TIMESTAMPTZ NOT NULL,
    raw                JSONB,
    UNIQUE (tenant_id, ebay_item_id)
);

CREATE TABLE finance_events (
    id                 BIGSERIAL PRIMARY KEY,
    tenant_id          UUID NOT NULL,
    event_id           TEXT NOT NULL,
    event_at           TIMESTAMPTZ NOT NULL,
    type               TEXT NOT NULL,    -- payout, fee, refund, etc.
    currency           CHAR(3) NOT NULL,
    amount             NUMERIC(12,2) NOT NULL,
    raw                JSONB,
    UNIQUE (tenant_id, event_id)
);

-- Shared outbox shape from libs/common-events. Forwarder lands in a follow-up.
CREATE TABLE outbox (
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
CREATE INDEX outbox_unforwarded_idx ON outbox (created_at) WHERE forwarded_at IS NULL;
