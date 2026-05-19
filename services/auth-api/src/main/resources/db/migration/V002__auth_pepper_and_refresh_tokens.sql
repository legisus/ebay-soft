-- Adds the columns/tables needed by the first real auth endpoints (#162).
--
-- pepper_version is a forward-compat column for issue #159 (password pepper +
-- versioning). Today it stays 0 = "no pepper applied"; #159 lands the HMAC
-- wrapper and starts writing 1+ on new hashes. Putting the column in now means
-- #159 doesn't need a schema migration of its own.

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS pepper_version INT NOT NULL DEFAULT 0;

-- Refresh tokens are server-side, one-time-use, revocable. We never store the
-- raw token — only a SHA-256 digest, so a DB read cannot resurrect a usable
-- refresh credential. The unique constraint on token_hash makes the
-- "find-or-revoke" lookup an O(1) index scan.

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  BYTEA NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX refresh_tokens_user_active_idx
    ON refresh_tokens (user_id)
    WHERE revoked_at IS NULL;
