# Architecture

ebay-soft is built as a set of **dedicated REST services**, each owning its data and exposing a versioned HTTP API. Services do not share database tables. The frontend talks to an **API gateway** that fans out to the right service.

## High-level diagram

```
                          ┌──────────────────────────┐
                          │   Cloudflare (WAF + TLS  │
                          │   + DDoS + CDN)          │
                          └────────────┬─────────────┘
                                       │
                          ┌────────────▼─────────────┐
                          │   Traefik (reverse proxy │
                          │   + Let's Encrypt)       │
                          └────────────┬─────────────┘
                                       │
                       ┌───────────────┴───────────────┐
                       │                               │
                ┌──────▼────────┐               ┌──────▼────────┐
                │  SPA (React,  │               │  API Gateway  │
                │  static)      │               │ (Spring Cloud │
                └───────────────┘               │   Gateway)    │
                                                └──────┬────────┘
                                                       │ JWT auth, rate-limit,
                                                       │ routing, request log,
                                                       │ OpenAPI aggregation
                                                       │
                ┌──────────────┬──────────────┬────────┴───────┬───────────────┬────────────────┐
                │              │              │                │               │                │
        ┌───────▼──────┐ ┌─────▼────────┐ ┌───▼──────────┐ ┌───▼──────────┐ ┌──▼────────────┐ ┌─▼──────────────┐
        │ auth-api     │ │ ebay-conn-api│ │ sync-api     │ │ accounting-  │ │ inventory-api │ │ repricer-api    │
        │ (MVC + VT,   │ │ (WebFlux,    │ │ (WebFlux,    │ │ api          │ │ (MVC + VT,    │ │ (MVC + VT,      │
        │  Postgres)   │ │  Redis)      │ │  R2DBC)      │ │ (MVC + VT,   │ │  Postgres)    │ │  Postgres)      │
        │              │ │              │ │              │ │  Postgres)   │ │               │ │                 │
        └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘ └───────────────┘ └─────────────────┘

        ┌───────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌────────────────┐
        │ analytics-api │ │ notif-api    │ │ billing-api  │ │ admin-api    │ │ ml-api          │
        │ (WebFlux,     │ │ (MVC + VT)   │ │ (MVC + VT,   │ │ (MVC + VT)   │ │ (Python /       │
        │  R2DBC,       │ │              │ │  Stripe)     │ │              │ │  FastAPI)       │
        │  SSE)         │ │              │ │              │ │              │ │                 │
        └───────────────┘ └──────────────┘ └──────────────┘ └──────────────┘ └────────────────┘

                                          ▼ shared infra ▼
                ┌──────────────────────────────────────────────────────────────┐
                │  Postgres 17  (one cluster, one schema per service)          │
                │  Redis 7      (cache, rate-limit buckets, ephemeral queues)  │
                │  MinIO        (objects: PDFs, CSVs, exports)                 │
                │  NATS JetStream OR Postgres LISTEN/NOTIFY (event bus)        │
                └──────────────────────────────────────────────────────────────┘
```

## Services and their contracts

The SPA stack (React 19 + TypeScript + ECharts, served by the gateway) is detailed in [FRONTEND.md](FRONTEND.md). The external eBay surfaces consumed by `ebay-conn-api` and `sync-api` are documented in [EBAY_API.md](EBAY_API.md).

| Service             | Stack                     | Owns                                                      | Public REST surface (excerpt)                          |
|---------------------|---------------------------|-----------------------------------------------------------|--------------------------------------------------------|
| `auth-api`          | Spring MVC + virtual threads | tenants, users, sessions, JWT keys, MFA enrolments (passkey/TOTP/SMS) | `POST /v1/auth/signup`, `POST /v1/auth/login`, `POST /v1/auth/refresh`, `POST /v1/auth/mfa/sms/start`, `POST /v1/auth/mfa/sms/verify`, `GET /v1/me`. Outbound: **Twilio Verify API** for SMS OTP. |
| `ebay-conn-api`     | Spring **WebFlux**        | eBay OAuth tokens (encrypted), eBay account metadata      | `GET /v1/oauth/ebay/start`, `GET /v1/oauth/ebay/callback`, `GET /v1/ebay-accounts`, `DELETE /v1/ebay-accounts/{id}` |
| `sync-api`          | Spring **WebFlux** + R2DBC| raw eBay payloads, sync watermarks, normalized orders/listings/fees | `POST /v1/sync/backfill`, `POST /v1/sync/notifications/ebay` (eBay → us), `GET /v1/sync/status` |
| `accounting-api`    | Spring MVC + VT           | P&L aggregations, COGS entries, exports                   | `GET /v1/pnl?from=&to=&groupBy=`, `POST /v1/exports/quickbooks`, `GET /v1/cogs` |
| `inventory-api`     | Spring MVC + VT           | SKUs, warehouses, stock levels                            | `GET /v1/skus`, `PATCH /v1/stock/{skuId}/{warehouseId}` |
| `repricer-api`      | Spring MVC + VT           | repricing rules, scheduled jobs, price-change history     | `POST /v1/repricer/rules`, `POST /v1/repricer/run`     |
| `analytics-api`     | Spring **WebFlux** + R2DBC| materialized read models, SSE feeds                       | `GET /v1/analytics/top-skus`, `GET /v1/analytics/stream` (SSE) |
| `notif-api`         | Spring MVC + VT           | notification preferences, outbound channels               | `POST /v1/notif/test`, `GET /v1/notif/prefs`           |
| `billing-api`       | Spring MVC + VT           | Stripe customers, subscriptions, plan limits, webhook ingest | `POST /v1/billing/checkout`, `POST /v1/billing/webhook/stripe` |
| `admin-api`         | Spring MVC + VT           | internal-only: impersonation, refunds, audit trail        | `GET /v1/admin/tenants` (staff role only)              |
| `ml-api`            | Python FastAPI            | forecasts, elasticity, "why isn't this selling" diagnostics | `POST /v1/ml/forecast`, `POST /v1/ml/diagnose-listing` |

Three rules everyone follows:

1. **No cross-service database joins.** Each service owns a Postgres **schema**; its DB user has access only to that schema. To get data from another service, call its REST API.
2. **Public API is versioned** (`/v1`). Breaking changes ship under `/v2` and the old version stays alive for at least one quarter.
3. **OpenAPI 3.1 spec is the contract** and is published by every service at `GET /openapi.json`. The gateway aggregates them for a unified developer portal. The developer-experience side (Swagger UI, Redoc portal, client generation, versioning policy) is detailed in [API_DOCS.md](API_DOCS.md).

## Reactive vs blocking — per service

Now that services are separate, each picks the stack that fits its workload:

| Workload type                                  | Stack                          | Services                              |
|-----------------------------------------------|--------------------------------|---------------------------------------|
| Fan-out to external APIs, streaming, backpressure | Spring WebFlux + `WebClient` + R2DBC | `ebay-conn-api`, `sync-api`, `analytics-api` |
| Standard CRUD, JPA/Hibernate, transactional   | Spring MVC + **virtual threads** (Java 21+) | everything else                     |

Virtual threads (`spring.threads.virtual.enabled=true`) make blocking I/O cheap, so MVC is the new default. WebFlux earns its complexity only where we genuinely stream or fan out at scale.

## Service-to-service communication

### Synchronous: REST + JSON

- The default. Clients are generated from each provider's OpenAPI using **openapi-generator**, producing a `*-client` artifact per service. Consumers add it as a Gradle dependency.
- Implemented with **Spring `RestClient`** (MVC services) or `WebClient` (WebFlux services).
- All inter-service calls carry:
  - `X-Tenant-Id` — propagated by the gateway from the JWT.
  - `X-Request-Id` / OTel `traceparent` — for distributed tracing.
  - A service-account JWT signed with our internal CA, validated by the callee.
- Timeouts are **explicit, never default**. Default `2s connect / 5s read`, tightened per call.
- Retries: idempotent GETs only, with exponential backoff and a circuit breaker (Resilience4j).
- Idempotency keys (`Idempotency-Key` header) required on every POST that mutates state.

### Asynchronous: event bus

For events that fan out to several services ("order synced", "stock low", "payout received"):

- **MVP: Postgres `LISTEN/NOTIFY`** wrapped in a tiny shared library. Zero new infra.
- **Phase 2: NATS JetStream** when we have >3 consumers per topic or need replay.
- **Phase 3 (only if needed): Kafka.**

Event shape is **CloudEvents 1.0** JSON, schema-versioned, published via the same `service-client` artifacts.

### What does NOT go through a service call

- The frontend never calls a service directly — only the gateway.
- A service never reads another service's database. Even read-only. **Ever.**
- Async events are not a way to bypass an API — if you need to read state, use the REST API.

## Data ownership

Each service owns one Postgres schema in **one shared Postgres cluster** (until scale forces a split):

```
postgres://...
├── auth         ← auth-api role only
├── ebay_conn    ← ebay-conn-api role only
├── sync         ← sync-api role only
├── accounting   ← accounting-api role only
├── inventory    ← inventory-api role only
├── repricer     ← repricer-api role only
├── analytics    ← analytics-api role only
├── notif        ← notif-api role only
├── billing      ← billing-api role only
└── admin        ← admin-api role only
```

Each service has its own Flyway migration history table inside its schema. Migrations run as the service starts.

**Tenant ID** is denormalized into every service's tables — it travels as `X-Tenant-Id` between services and is enforced via Row-Level Security in each schema (see [DATABASE.md](DATABASE.md)).

When scale demands it (Phase 3+), splitting a schema onto its own Postgres cluster is a swap of `spring.datasource.url`. No code changes.

## API gateway responsibilities

`api-gateway` (Spring Cloud Gateway) is the only inbound entry point for the SPA. It does:

- TLS termination (or pass-through from Cloudflare/Traefik).
- JWT validation, claims extraction, propagation as `X-Tenant-Id` / `X-User-Id` to downstream services.
- Per-tenant **rate limiting** (Redis token bucket).
- Request logging + tracing root-span creation.
- Routing by path: `/v1/auth/**` → `auth-api`, `/v1/pnl/**` → `accounting-api`, etc.
- CORS, security headers, body-size limits.
- Aggregated OpenAPI doc served at `/openapi.json`.

The gateway does **not** transform payloads or implement business logic. If you find yourself wanting to, that logic belongs in a service.

## Data flow — a typical seller's first day

1. Seller signs up at `ebay-soft.com` → SPA → `POST /v1/auth/signup` → **auth-api** creates tenant + user, returns JWT.
2. Seller clicks **Connect eBay** → SPA → `GET /v1/oauth/ebay/start` → **ebay-conn-api** returns eBay's authorize URL.
3. eBay redirects to `/v1/oauth/ebay/callback` → **ebay-conn-api** exchanges code for tokens, encrypts and stores them, **publishes** `ebay_account.connected` event.
4. **sync-api** consumes the event, kicks off the 24-month backfill. Streams orders/listings/fees from eBay using reactive `WebClient`, normalizes them, writes to `sync` schema, and publishes `order.synced` events for each batch.
5. **accounting-api** consumes `order.synced` events, recomputes the daily P&L for affected days, writes to `accounting.pnl_daily`.
6. **analytics-api** materializes chart-ready aggregates from its own read replica of relevant data (rebuilt from events).
7. SPA polls `GET /v1/sync/status` until done, then loads the dashboard from `accounting-api` and `analytics-api`.
8. Nightly: a scheduled job in `sync-api` calls **ml-api** to retrain per-seller forecasting and elasticity models.

## Cross-cutting concerns (shared across services)

| Concern        | Decision                                                                 |
|----------------|--------------------------------------------------------------------------|
| Logging        | `@Slf4j` + Logback JSON to stdout, scraped by Loki. Every log carries `tenantId`, `userId`, `traceId`. |
| Tracing        | OpenTelemetry SDK in every service, OTLP export to Tempo (or Grafana Cloud free tier). |
| Metrics        | Micrometer → Prometheus, one scrape target per service.                  |
| Auth between services | Internal JWT, signed by `auth-api`'s private key, validated by JWKS endpoint. |
| Config         | `application.yml` + per-env overrides + env vars; secrets via sops-encrypted files (Phase 2: Hashicorp Vault). |
| Migrations     | Flyway per service, each in its own schema.                              |
| Build          | **Monorepo, Gradle multi-project** — one root, one subproject per service, plus `common-*` libraries. One repo keeps refactors and shared-lib bumps tractable. |
| CI/CD          | GitHub Actions, matrix build, per-service Docker images, independent deploys to dev / stg / prod — full pipeline in [ENVIRONMENTS.md](ENVIRONMENTS.md). |
| Tests          | JUnit 5 + Testcontainers per service. **Contract tests** (Spring Cloud Contract or Pact) on every consumer/provider pair. |

## Failure modes we plan for

- **One service down** → gateway returns 503 for that capability, the rest of the app keeps working. SPA degrades gracefully (e.g. analytics widget shows "temporarily unavailable").
- **Slow downstream** → circuit breaker opens after 10 failures in 30s, half-opens after 30s.
- **eBay API down or rate-limited** → `sync-api` watermarks are durable; it resumes from where it stopped.
- **OAuth refresh fails** → `ebay-conn-api` marks the account `expired`, publishes `ebay_account.expired`, `notif-api` emails the seller.
- **Event consumer crash mid-batch** → events have idempotency keys; redelivery is safe.
- **DB lost** → see [INFRASTRUCTURE.md](INFRASTRUCTURE.md) backups + PITR.

## Tradeoffs we are accepting

Microservices come with real costs. We accept them and design to keep them small:

| Cost                                            | Mitigation                                                       |
|------------------------------------------------|-------------------------------------------------------------------|
| More moving parts to operate                    | One Hetzner box, Docker Compose, one Postgres cluster, one Redis. We don't multiply infra. |
| Distributed transactions are hard               | Embrace eventual consistency via events + idempotency. No 2PC.    |
| Local development is heavier                    | `docker compose up` brings the whole stack; profile excludes unused services. |
| Versioning APIs is real work                    | OpenAPI-first + generated clients + contract tests catch breaking changes in CI. |
| Latency penalty per hop                         | Keep call chains shallow (≤3 hops). Aggregate in the calling service rather than fan-out. |
| Observability is mandatory, not optional        | OTel from day one, dashboards before features.                    |

## What we explicitly do NOT do at MVP

- Kubernetes. Docker Compose on one Hetzner box runs 11 services comfortably; we move to k3s only when traffic actually justifies it.
- Service mesh (Istio/Linkerd). Resilience4j + OTel cover us at this scale.
- Per-service Postgres clusters. One cluster, one schema per service, separate DB users.
- Message broker beyond Postgres `LISTEN/NOTIFY` until we have >3 consumers per topic.
- Event sourcing / CQRS. Boring CRUD per service.
- API gateway as a smart proxy. The gateway routes; services do logic.
- Multi-region. One Falkenstein DC is enough through year 2.
