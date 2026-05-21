-- Temporary SKU master — lives here until inventory-api takes ownership in
-- Phase 3 (per docs/ROADMAP.md and the COGS table's comment in V001). For now
-- a flat (tenant_id, sku_code) row is enough to back the manual-COGS UI flow.
--
-- title and created_at let the dashboard render something nicer than the raw
-- SKU string. cost/currency intentionally NOT here — those live in cogs_entries
-- with their (effective_from) history.

CREATE TABLE skus (
    tenant_id    UUID NOT NULL,
    sku_code     TEXT NOT NULL,
    title        TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (tenant_id, sku_code)
);

CREATE INDEX skus_tenant_idx ON skus (tenant_id, created_at DESC);
