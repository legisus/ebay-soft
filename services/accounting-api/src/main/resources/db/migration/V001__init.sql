-- accounting schema — owns daily/monthly/listing P&L aggregates and per-tenant COGS overrides.
-- Money columns follow the two-column pattern from docs/DATABASE.md:
-- amounts as NUMERIC(p,s) + one shared `currency CHAR(3)` per row.

CREATE TABLE pnl_daily (
    tenant_id        UUID NOT NULL,
    date             DATE NOT NULL,
    group_by         TEXT NOT NULL CHECK (group_by IN ('day','month','year','listing','category')),
    group_key        TEXT NOT NULL,
    currency         CHAR(3) NOT NULL,
    revenue          NUMERIC(12,2) NOT NULL DEFAULT 0,
    fees             NUMERIC(12,2) NOT NULL DEFAULT 0,
    refunds          NUMERIC(12,2) NOT NULL DEFAULT 0,
    cogs             NUMERIC(12,2) NOT NULL DEFAULT 0,
    shipping         NUMERIC(12,2) NOT NULL DEFAULT 0,
    ads              NUMERIC(12,2) NOT NULL DEFAULT 0,
    net              NUMERIC(12,2) NOT NULL DEFAULT 0,
    last_event_id    TEXT,                    -- idempotency: last order.synced event applied
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (tenant_id, date, group_by, group_key)
);

CREATE INDEX pnl_daily_tenant_date_idx ON pnl_daily (tenant_id, date DESC);

-- Per-SKU manual COGS — temporary home until inventory-api takes ownership in Phase 3.
CREATE TABLE cogs_entries (
    tenant_id        UUID NOT NULL,
    sku_code         TEXT NOT NULL,
    currency         CHAR(3) NOT NULL,
    cost             NUMERIC(12,2) NOT NULL,
    effective_from   DATE NOT NULL,
    PRIMARY KEY (tenant_id, sku_code, effective_from)
);
