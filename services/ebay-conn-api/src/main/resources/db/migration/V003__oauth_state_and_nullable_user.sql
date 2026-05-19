-- #164 — make the OAuth flow real:
--   * oauth_states gains marketplace_id so the callback can recover which eBay
--     marketplace this seller is connecting (US/UK/DE/…). Default 'EBAY_US'
--     for any in-flight rows (there are none in prod yet — table is empty).
--   * oauth_states.redirect_uri can be NULL — the redirect_uri is derived from
--     EbayProperties at flow time; the column stays for forward-compat with a
--     future multi-domain deploy but isn't required.
--   * ebay_accounts.ebay_user_id becomes nullable. We persist the connection
--     before calling eBay's identity API; a follow-up issue (see #166 thread)
--     wires that call and backfills the column.

ALTER TABLE oauth_states
    ADD COLUMN IF NOT EXISTS marketplace_id TEXT NOT NULL DEFAULT 'EBAY_US';

ALTER TABLE oauth_states
    ALTER COLUMN redirect_uri DROP NOT NULL;

ALTER TABLE ebay_accounts
    ALTER COLUMN ebay_user_id DROP NOT NULL;

-- Older unique constraint required ebay_user_id; allow NULL by relaxing it.
-- Two rows with the same tenant_id + NULL ebay_user_id are still unique under
-- Postgres' NULL-is-distinct default, which is the behavior we want here.
