-- ebay_conn schema — owns connected eBay accounts and (encrypted) OAuth tokens.
-- See docs/DATABASE.md for the per-service schema rule.

CREATE TABLE ebay_accounts (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                UUID NOT NULL,
    marketplace_id           TEXT NOT NULL,           -- EBAY_US, EBAY_DE, ...
    ebay_user_id             TEXT NOT NULL,
    refresh_token_enc        BYTEA NOT NULL,          -- AES-256-GCM, KEK from env
    access_token_expires_at  TIMESTAMPTZ,
    status                   TEXT NOT NULL DEFAULT 'connected'
                              CHECK (status IN ('connected', 'expired', 'revoked')),
    connected_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, ebay_user_id)
);

CREATE INDEX ebay_accounts_tenant_idx ON ebay_accounts (tenant_id);

-- OAuth state values minted by /v1/oauth/ebay/start and consumed by /callback. Short-lived.
CREATE TABLE oauth_states (
    state            TEXT PRIMARY KEY,
    tenant_id        UUID NOT NULL,
    redirect_uri     TEXT NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at       TIMESTAMPTZ NOT NULL
);

CREATE INDEX oauth_states_expires_idx ON oauth_states (expires_at);
