# Observability

How we see what's happening across 12 services in production. The three pillars (metrics, logs, traces) plus errors, with one canonical tool per pillar and a single Grafana pane of glass.

The infrastructure shape (containers, env split, resource budget) lives in [INFRASTRUCTURE.md](INFRASTRUCTURE.md) and [ENVIRONMENTS.md](ENVIRONMENTS.md); this doc is about the **observability pipeline itself** — what gets emitted, what stores it, what queries it, what alerts on it.

---

## The stack

```
                    ┌──────────────────────────────────────┐
                    │              GRAFANA                 │
                    │   (dashboards + alerts + on-call)    │
                    └────┬────────────┬────────────┬───────┘
                         │            │            │
                ┌────────▼──┐  ┌──────▼──┐  ┌─────▼──────┐
                │Prometheus │  │  Loki   │  │   Tempo    │
                │ (metrics) │  │ (logs)  │  │  (traces)  │
                └────────▲──┘  └──────▲──┘  └─────▲──────┘
                         │            │            │
                         │ scrape     │ push       │ OTLP
                         │            │            │
                ┌────────┴────────────┴────────────┴─────┐
                │           Every service emits          │
                │  Micrometer / JSON logs / OTel spans    │
                └─────────────────────────────────────────┘

                                                      (errors)
                                                ┌──────────────┐
                                                │   Sentry     │
                                                │  (Team plan) │
                                                └──────────────┘
```

| Pillar | Tool | Hosting | Why this one |
|---|---|---|---|
| **Metrics** | Prometheus | Self-hosted (one Docker container on the Hetzner box) | Industry default; Micrometer integrates trivially with every Spring Boot app; zero per-metric cost |
| **Logs** | Loki + Promtail | Self-hosted | Cheap (label-indexed, not full-text), queries natively in Grafana, scales linearly with disk |
| **Traces** | Tempo | Self-hosted | OTLP-native, Grafana-integrated, doesn't index trace bodies (cheap storage) |
| **Errors** | Sentry | Hosted (Team plan, ~$26/mo) | Stack-trace grouping, release tracking, source-map upload — not worth self-hosting |
| **Dashboards + alerts** | Grafana OSS | Self-hosted, one instance per env | Single pane of glass over all three pillars |
| **Synthetic monitoring** | UptimeRobot or BetterStack | Hosted | Hits prod from external regions; alerts on multi-region failure |

### Self-hosted vs Grafana Cloud — the decision

Grafana Cloud free tier gives 10k series, 50 GB logs, 50 GB traces — enough for our first year. **But** it ties our observability to a vendor's free-tier limits and requires shipping data out of our EU region, which complicates the GDPR posture in [LEGAL.md](LEGAL.md).

**Decision:** self-host the full stack on the Hetzner box. Resource budget already allocated (6 GB RAM total for Grafana / Loki / Tempo / Prometheus / Promtail per [INFRASTRUCTURE.md](INFRASTRUCTURE.md)). Migrate to Grafana Cloud only if a specific feature (managed alerting, on-call schedules) forces it.

Sentry stays hosted — self-hosting Sentry is a significant operational tax for marginal value at our scale.

---

## What every service emits

Every Spring Boot service (and `ml-api`) ships with three instrumentations, configured once in `libs/common-web`:

### Metrics — Micrometer → Prometheus

```java
// Auto-configured by libs/common-web
@Bean
MeterRegistryCustomizer<MeterRegistry> commonTags(@Value("${spring.application.name}") String svc,
                                                   Environment env) {
    return registry -> registry.config().commonTags(
        "service", svc,
        "env", env.getActiveProfiles()[0],
        "version", buildInfo.getVersion()
    );
}
```

Scrape endpoint: `GET /actuator/prometheus` on every service. Prometheus discovers them via Docker labels in `compose.prod.yml`.

**Out-of-the-box metrics** (Spring Boot Actuator + Micrometer):

- JVM: heap / non-heap / GC pauses / virtual-thread count
- HTTP server: request count, p50/p95/p99 latency, status-code distribution, per-URI
- WebClient / RestClient outbound: same metrics for downstream calls (eBay, other services)
- HikariCP / R2DBC pool: active / idle / max
- Resilience4j: circuit-breaker state, retry counts
- Custom business metrics (named `ebaysoft_*`):
  - `ebaysoft_sync_orders_total{tenant, marketplace}`
  - `ebaysoft_pnl_recomputed_total{tenant, reason}`
  - `ebaysoft_repricer_decisions_total{tenant, strategy, action}`
  - `ebaysoft_chargeback_evidence_assembled_total{outcome}`
  - `ebaysoft_signup_total{plan, source}`
  - `ebaysoft_mrr_dollars{plan}` (gauge, refreshed every 5 min by `billing-api`)

### Logs — JSON to stdout → Promtail → Loki

Structured logging only. The `@Slf4j` fluent API ([BACKEND.md → Logging](BACKEND.md)) ensures every log line is queryable as fields, not regex.

Standard fields on every line (added by Logback MDC filter in `libs/common-web`):

- `service` — `auth-api`, `sync-api`, …
- `env` — `dev` / `stg` / `prod`
- `version` — git SHA
- `tenantId` — propagated from `X-Tenant-Id`
- `userId` — propagated from `X-User-Id`
- `traceId`, `spanId` — from OTel context, enables jumping log → trace
- `httpMethod`, `httpStatus`, `path` — for request logs

Promtail tails the Docker JSON log driver, adds labels (`service`, `env`), pushes to Loki. **Labels stay low-cardinality** (no `tenantId` as a label — that's a field). High-cardinality data goes in the log JSON body where Loki's structured query (`{service="sync-api"} | json | tenantId="..."`) handles it without exploding the index.

### Traces — OpenTelemetry → Tempo

Every service uses the OpenTelemetry Java agent (auto-instrumentation) plus `libs/common-web` for manual spans on business operations.

- Inbound: gateway creates the root span, propagates `traceparent` to every downstream service.
- Outbound: every service-to-service call, every eBay/Stripe/Twilio call, every DB query gets a span.
- Sampling: head-based, **10% in prod**, **100% in stg** and dev. Errors always sampled (tail-based via OTel collector).
- Trace export to Tempo via OTLP gRPC. Storage ~30 days in prod.

### Errors — Sentry

Every uncaught exception, every `log.error()`, every 5xx response gets reported to Sentry by the Sentry SDK + Logback appender.

- Release tagging tied to git SHA (`SENTRY_RELEASE` env var).
- Source maps uploaded for the SPA on each release.
- PII scrubbing on by default: emails, tokens, eBay refresh tokens, phone numbers — all redacted before send. (Verified by a test in CI.)
- Issue assignment auto-routes by `service` tag to the service owner.

---

## Dashboards we ship at launch

Folder structure in Grafana:

```
ebay-soft/
├── 00 - Overview
│   ├── System Health (all services)
│   └── Business KPIs (signups, MRR, sync volume)
├── 10 - Services
│   ├── auth-api Golden Signals
│   ├── ebay-conn-api Golden Signals
│   ├── sync-api Golden Signals
│   └── ... (one per service)
├── 20 - Infrastructure
│   ├── Postgres
│   ├── Redis
│   ├── MinIO
│   └── Hetzner host (node-exporter)
├── 30 - Workflows
│   ├── eBay Sync Pipeline (end-to-end trace volume + lag)
│   ├── P&L Recompute
│   ├── Repricer Runs
│   └── Chargeback Defense (Phase 8+)
└── 90 - Per-tenant troubleshooting (parameterized by tenantId)
```

### The "Golden Signals" template (one per service)

Every service gets a generated dashboard with the four signals from Google's SRE book:

1. **Latency** — p50 / p95 / p99 for HTTP requests, broken out by route
2. **Traffic** — requests/second, success-vs-error split
3. **Errors** — 5xx rate, exception count, Sentry incidents linked
4. **Saturation** — JVM heap %, thread pool %, DB pool %, GC pause time

Plus per-service custom panels for whatever matters:

- `auth-api`: signups/min, login failure rate, 2FA prompts, SMS OTP send count + Twilio cost
- `sync-api`: eBay API calls/min by endpoint, eBay 429 rate, watermark lag per seller, backfill jobs in flight
- `accounting-api`: P&L recomputes/min, end-to-end recompute latency, fee-reconciliation gaps detected
- `ml-api`: forecast latency, model age, request count per endpoint

All dashboards are **provisioned from JSON in the repo** (`infra/grafana/dashboards/*.json`), not edited live. Editing live is a flake factory; we keep them in git so they version, diff, and code-review like everything else.

### Business dashboard

The one the founder looks at every morning:

- **Free signups** / **trial activations** / **paid conversions** (24h, 7d, 30d)
- **MRR** (refreshed every 5 min from `billing-api` Stripe sync)
- **Active tenants** (sessions in last 24h)
- **Top 10 tenants by order volume** (last 24h, anonymized in screenshots)
- **eBay sync health** — % of tenants synced in last hour
- **Seller Shield activations** — orders flagged 🔴 in last 24h
- **Chargeback cases handled** + **$ recovered cumulative** (Phase 8+)

Drives the daily Telegram brief (IDEAS #16).

### Per-tenant troubleshooting

Parameterized dashboard takes `tenantId` as a Grafana variable. Useful for support: "show me everything happening for this customer." Joins logs (Loki) + traces (Tempo) + business metrics (Prometheus) on `tenantId`.

---

## Alerting

Two severities, both managed in Grafana Alerting (the OSS alerting engine, not a separate Alertmanager):

### Alerts that **page** (Severity 1 — wake someone up)

| Alert | Condition | Where it goes |
|---|---|---|
| Service down | `up == 0` for 5 min on any service | Telegram + email + PagerDuty (when staffed 24/7) |
| 5xx rate | >1% of requests over 5 min on any service | Same |
| Latency | p95 > 2s sustained for 10 min on `api-gateway` | Same |
| DB connection saturation | Hikari/R2DBC pool >80% for 10 min | Same |
| Disk | >85% on the Hetzner box | Same |
| TLS cert | expires in <14 days | Same |
| Stripe webhook failure | >5 consecutive failed deliveries | Same |
| eBay refresh-token mass failure | >10 tenants failing token refresh in 1h | Same |

### Alerts that **ticket** (Severity 2 — review in normal hours)

| Alert | Condition | Where it goes |
|---|---|---|
| Daily sync regression | sync job duration up >50% WoW | GitHub Issue (auto-created) |
| eBay 429 rate | >10/hour on `sync-api` | GitHub Issue |
| Chargeback packet failure | >2 in 24h failed to assemble | GitHub Issue |
| Twilio cost spike | >$50/day SMS spend | Email to founder |
| Flaky-test count | new flakes seen this week | GitHub Issue, see [TESTING.md](TESTING.md) flake policy |

### Alert hygiene rules

- Every alert has a **runbook URL** in its annotations. No runbook = no alert.
- Every page-grade alert has been **manually triggered at least once** to verify it actually pages.
- **Alert fatigue is the enemy.** Quarterly review: if an alert fired but didn't drive action, raise the threshold or delete it.
- **Maintenance windows** silence alerts via a Grafana annotation, not by editing rules.

---

## On-call workflow

At MVP — solo founder — there's no rotation. Personal phone gets paged via Telegram bot (cheap, no per-seat fee like PagerDuty).

At >€10k MRR and team >2 — proper on-call:

- PagerDuty (or Better Stack On-Call, cheaper) with weekly rotation
- Runbook per alert lives in the repo (`/ops/runbooks/<alert-name>.md`)
- Post-incident review (blameless) within a week of every S1
- Quarterly fire drill: deliberately kill a service, verify alert fires, runbook works

---

## Log retention & cost

Loki costs scale with **storage**, not query volume.

| Env | Retention | Approx storage / day |
|---|---|---|
| prod | 30 days | ~2 GB compressed (12 services × 100 MB raw) |
| stg | 7 days | ~500 MB |
| dev | (local only) | — |

Total prod log storage: ~60 GB. Fits comfortably on the Hetzner box's NVMe. Long-term archive (1 year) for audit/compliance: weekly bzip2 to Hetzner Storage Box.

Sampling is **NOT** applied to logs at our scale. Once we exceed ~50 GB/day we'll reconsider.

---

## Cross-references

- Container topology, host resource budget → [INFRASTRUCTURE.md](INFRASTRUCTURE.md)
- Per-env Grafana / Sentry separation → [ENVIRONMENTS.md](ENVIRONMENTS.md)
- Logging conventions (`@Slf4j` fluent API, mask filter for secrets) → [BACKEND.md](BACKEND.md)
- Secrets that the observability pipeline uses (Sentry DSN, Grafana Cloud token if applicable) → [SECURITY.md → Secrets](SECURITY.md#secrets)
- Synthetic monitoring (Playwright in prod) and load testing (Gatling) → [TESTING.md](TESTING.md)
