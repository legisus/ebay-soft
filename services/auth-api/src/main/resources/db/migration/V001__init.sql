-- Initial auth schema for auth-api. Lives entirely in the `auth` Postgres schema (see
-- docs/DATABASE.md and docs/ARCHITECTURE.md — one schema per service, no cross-schema joins).
-- Production columns (TOTP, SMS-OTP, password pepper, etc.) are added by subsequent migrations
-- as the corresponding features land.

CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE tenants (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT NOT NULL,
    plan        TEXT NOT NULL DEFAULT 'free' CHECK (plan IN ('free','starter','growth','pro','scale','agency')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    email                CITEXT NOT NULL UNIQUE,
    password_hash        TEXT,
    role                 TEXT NOT NULL DEFAULT 'owner' CHECK (role IN ('owner','member','viewer')),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_login_at        TIMESTAMPTZ
);

CREATE INDEX users_tenant_idx ON users (tenant_id);
