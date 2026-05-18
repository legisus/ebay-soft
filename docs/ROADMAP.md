# Roadmap

A realistic path from "empty repo" to "paying customers" in ~6 months, then year-one expansion. Sequenced around the microservices architecture in [ARCHITECTURE.md](ARCHITECTURE.md) — each phase ships a small set of services end-to-end rather than building one big thing. The features each phase delivers are catalogued (with their owning service) in [FEATURES.md](FEATURES.md); the eBay API surfaces consumed in Phase 1 are in [EBAY_API.md](EBAY_API.md).

Assumptions: one full-time founder-engineer, one part-time designer (10h/week), one part-time copywriter for marketing pages. If you're solo, double the dates.

**Sequencing rule:** add one service only when the previous one is observable, contract-tested, and behind a green CI matrix. Twelve services in a hurry is how startups die; twelve services brought up one at a time is just twelve small Spring Boot apps.

---

## Pre-development (week -2 to 0)

Grouped by area; everything here unblocks Phase 0 or earlier.

**Domain & eBay**

| Item                                                         | Status |
|--------------------------------------------------------------|--------|
| Register `ebay-soft.com` (+ defensive: `ebaysoft.com`, `.app`, `.eu` if affordable) | TODO   |
| Register **eBay Developers Program** account                 | TODO   |
| Apply for **eBay Sandbox** keyset                            | TODO   |
| Apply for **eBay Production** keyset (early — review takes time) | TODO |

**Infrastructure & tooling**

| Item                                                         | Status |
|--------------------------------------------------------------|--------|
| Provision Hetzner AX102 in Falkenstein                       | TODO   |
| Set up Cloudflare account, transfer DNS                      | TODO   |
| Set up GitHub org `ebay-soft`, private monorepo, Projects v2 | TODO   |
| Configure GitHub Actions + a self-hosted runner on the Hetzner box (see [ENVIRONMENTS.md](ENVIRONMENTS.md)) | TODO   |

**Company & finance**

| Item                                                         | Status |
|--------------------------------------------------------------|--------|
| Reserve company entity (LLC / GmbH / sp. z o.o. — depends on your location) | TODO |
| Open business bank account                                   | TODO   |
| Retain an accountant familiar with SaaS + VAT OSS            | TODO   |
| Set up Stripe account; register VAT OSS via Stripe Tax       | TODO   |

**Legal & governance** (see [LEGAL.md](LEGAL.md) and [GOVERNANCE.md](GOVERNANCE.md))

| Item                                                         | Status |
|--------------------------------------------------------------|--------|
| Draft Privacy Policy + Terms of Service (generator first, lawyer review before Phase 4) | TODO |
| Draft Cookie Policy (even just "essential only")             | TODO   |
| Draft DPA template (IAPP or Vanta starting point)            | TODO   |
| Publish sub-processor list and Acceptable Use Policy         | TODO   |
| Decide license for the main repo (BSL 1.1 → Apache 2.0 in 4y recommended) | TODO |
| File EU + US trademark applications for the "ebay-soft" wordmark | TODO |
| Get D&O + cyber-liability insurance quotes (purchase decision at $10k MRR) | TODO |

---

## Phase 0 — Foundations & first two services (week 1–4)

**Goal:** the platform skeleton is alive. The gateway routes, one service answers, the SPA logs in.

**Services delivered:** `api-gateway`, `auth-api`.
**Shared libs delivered:** `common-domain` (incl. `Money`), `common-web`, `common-security`, `common-events`, `common-test`.

- [ ] Gradle monorepo: `buildSrc/` convention plugins, `gradle/libs.versions.toml`, `services/*` + `libs/*` + `clients/*` layout (see [BACKEND.md](BACKEND.md))
- [ ] `libs/common-domain` — `Money` record, `TenantId`, `UserId`, `SkuCode`, `Currency` helpers (with unit tests on rounding)
- [ ] `libs/common-web` — Jackson `Money` serializer/deserializer, error model, OpenAPI config, OTel auto-config
- [ ] `libs/common-security` — service-account JWT issuer/verifier, JWKS client, gateway header parser
- [ ] `libs/common-events` — CloudEvents envelope, outbox table DDL fragment, `LISTEN/NOTIFY` publisher + consumer skeletons
- [ ] **`api-gateway`** — Spring Cloud Gateway: routing config, JWT validation, rate limiting (Redis token bucket), CORS, security headers, aggregated `/openapi.json`
- [ ] **`auth-api`** — Spring MVC + virtual threads. Tenant + user + session tables (its own `auth` schema), sign-up, login, TOTP, JWT issuance, password reset. Flyway migrations run on boot.
- [ ] OpenAPI spec checked in (`services/auth-api/openapi.yaml`), CI check that `GET /openapi.json` matches the committed file.
- [ ] OpenAPI client published locally (`clients/auth-api-client`) and consumed by the gateway.
- [ ] React 19 + Vite + Tailwind + shadcn/ui + TanStack Query scaffolding. Login screen against `auth-api` via the gateway.
- [ ] One ECharts widget (hardcoded data) to prove the chart pipeline.
- [ ] `compose.prod.yml` aggregates each service's `compose.partial.yml` — current count: 2 services + postgres + redis + traefik + observability.
- [ ] CI matrix: build/test/lint per changed service, SBOM, trivy scan.
- [ ] OpenTelemetry from day one: traces from SPA → gateway → `auth-api` visible in Tempo, JSON logs in Loki, metrics in Grafana.
- [ ] Hetzner box configured, first automated deploy of two services works.

**Definition of done:** I can sign up at `staging.ebay-soft.com`, log in with 2FA via `auth-api`, see a "hello, ${user.email}" dashboard with one fake chart, and watch the full trace in Tempo.

---

## Phase 1 — eBay connection & sync (week 5–7)

**Goal:** the seller OAuths into eBay and their data lands in our database.

**Services delivered:** `ebay-conn-api`, `sync-api` (both WebFlux).

- [ ] **`ebay-conn-api`** (own `ebay_conn` schema):
  - [ ] OAuth 2.0 authorization-code flow (sandbox + production keyset)
  - [ ] Encrypted refresh-token storage (AES-256-GCM, KEK from env)
  - [ ] Token-refresh worker with watchdog
  - [ ] **Marketplace Account Deletion** notification endpoint (eBay-mandatory)
  - [ ] Publishes `ebay_account.connected` / `ebay_account.expired` events via outbox → `LISTEN/NOTIFY`
- [ ] **`sync-api`** (own `sync` schema, WebFlux + R2DBC):
  - [ ] Reactive `WebClient` with rate-limit filter, retry filter, per-tenant token bucket
  - [ ] Initial backfill (orders, listings, finance transactions — last 24 months) as a resumable `Flux` pipeline with per-stream watermarks
  - [ ] Incremental sync via eBay Platform Notifications webhook
  - [ ] Publishes `order.synced`, `listing.synced`, `finance_event.recorded` events
  - [ ] All money fields stored via the `Money` two-column pattern from [DATABASE.md](DATABASE.md)
- [ ] Consumer-driven contract tests: `sync-api` (provider) ↔ `accounting-api` placeholder (consumer) so the contract is real before Phase 2 starts.
- [ ] Wiremock recordings of real sanitized eBay responses as integration fixtures.
- [ ] Sync-status SSE endpoint on `sync-api` for the frontend "syncing…" progress bar.

**Definition of done:** I connect my eBay test account through `ebay-conn-api`, kick off sync, and 5 minutes later `sync-api`'s schema has all my orders. Tempo shows the full distributed trace including outbound eBay calls.

---

## Phase 2 — P&L MVP & analytics (week 8–11)

**Goal:** the first feature a seller would pay for.

**Services delivered:** `accounting-api`, `analytics-api`.

- [ ] **`accounting-api`** (own `accounting` schema, Spring MVC + virtual threads):
  - [ ] Consumes `order.synced` / `finance_event.recorded` events, recomputes affected days of `pnl_daily` (idempotent, keyed by event id)
  - [ ] P&L math: revenue − fees − refunds − COGS − shipping − ads, every amount a `Money`, every division explicit on rounding (`HALF_EVEN`)
  - [ ] Per day / week / month / year / listing / category endpoints (`GET /v1/pnl?groupBy=...`)
  - [ ] `Money` JSON serialization (`{"amount":"123.45","currency":"USD"}`) — verified by contract test
  - [ ] CSV + XLSX + PDF report export (PDFs to MinIO, signed URLs returned)
  - [ ] SKU master + manual COGS entry (will live in `inventory-api` from Phase 3 — owned here temporarily, then migrated)
- [ ] **`analytics-api`** (own `analytics` schema, WebFlux + R2DBC + SSE):
  - [ ] Materializes chart-ready aggregates from `order.synced` events into its own read model
  - [ ] Endpoints feed the dashboard widgets (top SKUs, dead-stock, hourly heatmap, category mix treemap)
  - [ ] SSE stream of live-update events for the open dashboard
- [ ] Dashboard wired up: 6–8 ECharts widgets, **Profit Mode toggle** (see [IDEAS.md](IDEAS.md) #11), TypeScript types generated from each service's OpenAPI.
- [ ] ArchUnit rule active across all services: no `double`/`float`, no naked `BigDecimal#divide`.

**Definition of done:** I see my real net profit for the last 12 months across multiple views, generated by `accounting-api`, charted via data from `analytics-api`, and I can hand my accountant a PDF.

---

## Phase 3 — Inventory & alerts (week 12–13)

**Goal:** sellers stop running out of stock without warning.

**Services delivered:** `inventory-api`, `notif-api`.

- [ ] **`inventory-api`** (own `inventory` schema, MVC + VT):
  - [ ] SKU master migrated from `accounting-api` (one-time data move + cutover)
  - [ ] Multi-warehouse stock tracking (`stock_levels` table)
  - [ ] Low-stock evaluation worker → publishes `stock.low` events
  - [ ] Dead-stock report endpoint
  - [ ] Bulk SKU import / export (CSV)
  - [ ] COGS in `Money` (with `cost_currency` column per [DATABASE.md](DATABASE.md))
- [ ] **`notif-api`** (own `notif` schema, MVC + VT):
  - [ ] Consumes `stock.low`, `ebay_account.expired`, `payout.received` events
  - [ ] Email (Postmark or Resend), in-app notification feed
  - [ ] Per-tenant notification preferences
- [ ] `accounting-api` now reads SKU/COGS data from `inventory-api` via generated client (no cross-schema joins).

**Definition of done:** SKU goes below reorder point → `inventory-api` publishes `stock.low` → `notif-api` sends an email + in-app notification, fully traced.

---

## Phase 4 — Billing & beta launch (week 14–15)

**Goal:** Stripe is taking money. Private beta opens.

**Services delivered:** `billing-api`, `admin-api`.

- [ ] **`billing-api`** (own `billing` schema, MVC + VT):
  - [ ] Stripe Checkout + Customer Portal integration
  - [ ] Stripe webhook receiver (`POST /v1/billing/webhook/stripe`) — verified signatures, idempotency keys, replay-safe
  - [ ] Plan-limit enforcement: publishes `subscription.changed` events; `auth-api` gates feature access on the JWT plan claim refreshed at login
  - [ ] 14-day Pro trial
  - [ ] Tax via Stripe Tax
- [ ] **`admin-api`** (own `admin` schema, MVC + VT, staff-only auth role):
  - [ ] Tenant search, impersonation (with audit), refund issuance, support tooling
  - [ ] Aggregated audit-log view (reads `audit_log` rows from each service via their public API)
- [ ] **SMS-OTP via Twilio Verify** added to `auth-api`:
  - [ ] Twilio account provisioned, Verify Service configured, account SID + auth token in sealed secrets
  - [ ] Phone columns on `users` (E.164, verified_at, failed_attempts, locked_until)
  - [ ] Endpoints: enrol, verify, remove phone; step-up login flow when SMS is the chosen factor
  - [ ] Rate limits (3/15 min/phone, 10/day) enforced via Bucket4j + Redis before calling Twilio
  - [ ] Country gate, CAPTCHA on sign-up, SIM-swap webhook from Twilio handled
  - [ ] User-facing copy explains SMS is weaker than TOTP/passkey; high-value actions still require the stronger factor
- [ ] Marketing site: landing, pricing, blog scaffolding, ToS, Privacy, DPA
- [ ] **Free eBay Fee Calculator** built into marketing site (SEO, see [IDEAS.md](IDEAS.md) #15)
- [ ] Onboarding wizard for first-run sellers
- [ ] Open private beta to 20 hand-picked sellers

**Definition of done:** Stripe is taking real money from 5+ paying customers across two plan tiers. 7 services in production.

---

## Phase 5 — ML insights v1 (month 4)

**Goal:** the screenshots that get shared.

**Services delivered:** `ml-api` (Python FastAPI).

- [ ] **`ml-api`** containerized alongside the JVM services:
  - [ ] `POST /v1/ml/forecast` — Prophet/LightGBM demand forecast per SKU
  - [ ] `POST /v1/ml/diagnose-listing` — "Why this listing isn't selling" report (see [IDEAS.md](IDEAS.md) #2)
  - [ ] Receives a service-account JWT issued by `auth-api`, validates via JWKS
  - [ ] OTel-instrumented; metrics in Prometheus alongside the JVM services
- [ ] `accounting-api` builds the **Profit Black Box** Sankey (IDEAS.md #1) — frontend viz, no new service.
- [ ] `analytics-api` adds the **Promoted-Listings ROI** dashboard (IDEAS.md #7).
- [ ] Nightly batch in `sync-api` calls `ml-api` to retrain per-seller models.

---

## Phase 6 — Repricer & listing optimizer (month 5)

**Goal:** sellers stop racing to the bottom; listings start scoring themselves.

**Services delivered:** `repricer-api`.

- [ ] **`repricer-api`** (own `repricer` schema, MVC + VT):
  - [ ] Rule-based engine with **margin floor** safety (IDEAS.md #3) — pulls margin from `accounting-api`, never reprices below the floor
  - [ ] Dry-run mode (no price push, only logs proposed changes)
  - [ ] Scheduled-run worker; price changes pushed to eBay via `ebay-conn-api` proxy endpoint (so `ebay-conn-api` remains the only service holding eBay tokens)
- [ ] Listing optimizer features bolt onto **`sync-api`** (no new service):
  - [ ] Title scorer (rule-based)
  - [ ] Image audit (count, resolution, white-background heuristic)
  - [ ] Aspect completeness against category taxonomy
- [ ] AI title-rewrite with brand-voice config (IDEAS.md #13) — calls an LLM provider from `sync-api`, gated by tenant plan.

---

## Phase 7 — Public launch (month 6)

**Goal:** the press release.

- [ ] External **pen test** completed; any HIGH findings fixed.
- [ ] Press / blog launch — Hacker News, IndieHackers, eBay seller communities.
- [ ] Telegram bot daily brief (IDEAS.md #16) — new outbound channel on `notif-api`.
- [ ] All 5 free SEO tools live on marketing site (IDEAS.md #15).
- [ ] Migration discount campaign targeting A2X / LMB / Sellbrite users.
- [ ] **All 11 Java services + ml-api + api-gateway live, each independently deployable**, contract tests across every consumer/provider pair.
- [ ] **Seller Shield announced as the first major post-launch update** (Phase 8 below) — sign-up waitlist on the marketing site, "ships in 6 weeks" promise. Doubles the upgrade pressure for trial users.

**Target by end of month 6:** 100 paying customers, ~$3k MRR, 1k free signups.

---

## Phase 8 — Seller Shield (month 7–8)

**Goal:** ship the forum-validated defensive feature pack that no incumbent has. Every component is an extension of an existing service — no new services in this phase.

**Why first after launch:** the 2026-05 forum research (eBay community + ValueAddedResource + Closo) named this category — Empty Box / Address-Change / Broken Replica / chargebacks — as the most-recurring seller pain. Shipping it ~6 weeks after public launch turns "ebay-soft" into "the tool that actually defends my account," which is the screenshot we want shared on r/eBaySellerAdvice.

**Features delivered** (cross-reference [FEATURES.md](FEATURES.md) §12 and [IDEAS.md](IDEAS.md) #19–#20):

- [ ] **Buyer-risk score per order** (FEATURES 12.1) — `analytics-api` extension:
  - [ ] Score model: feedback age, dispute history, return-rate, account age, velocity, prior chargeback count
  - [ ] Materialized `buyer_risk` view per (tenant, buyer_pseudonym), refreshed on each `order.synced` event
  - [ ] 🟢/🟡/🔴 badge rendered on orders page; sortable, filterable
  - [ ] `notif-api` consumes `order.high_risk` events; surfaces email + in-app alert
- [ ] **Address-change interceptor** (FEATURES 12.2) — `sync-api` extension:
  - [ ] eBay buyer-seller messaging ingestion (Sell Account API + Trading API where required)
  - [ ] Light NLP classifier ("can you ship to a different address?") with a hand-curated training set
  - [ ] Order hold on detected change + warning banner in UI explaining eBay-protection voiding
  - [ ] One-click "refuse" path; explicit "override (logged)" path with audit-log entry
- [ ] **Pack-out evidence capture** (FEATURES 12.3) — `inventory-api` extension + small PWA workflow:
  - [ ] PWA-only capture screen accessible from any phone browser; no native app needed
  - [ ] Records timestamped video (≤60s) with order-ID + SKU + ship-to-city overlay rendered into the video
  - [ ] Upload streams to MinIO with signed URL; metadata row in `inventory_api.packout_evidence`
  - [ ] Auto-attach to any subsequent dispute on that order (via `sync-api` event subscription)
  - [ ] Storage policy: kept 18 months from order date (covers eBay + chargeback windows), then signed-URL revocation + delete
- [ ] **Bank-chargeback defense workflow** (FEATURES 12.4) — `billing-api` extension:
  - [ ] Stripe webhook handler for `charge.dispute.created` / `charge.dispute.funds_withdrawn`
  - [ ] Per-card-network representment template (Visa, Mastercard, Amex, Discover)
  - [ ] Auto-assembly: tracking + signature from `sync-api`, listing photos via `ebay-conn-api` cache, pack-out video from MinIO, P&L impact from `accounting-api`, buyer history from `analytics-api`
  - [ ] Pre-filled evidence packet downloadable as a single PDF + image bundle the seller submits to Stripe / their merchant processor
  - [ ] Case-tracking dashboard: open / submitted / won / lost; outcome-feedback loop to refine the template
- [ ] **"Seller Shield" marketing page** at `ebay-soft.com/seller-shield` with the screenshot we built it for (orders list with 🟢/🟡/🔴 badges + address-change banner).
- [ ] **Case studies**: 3 anonymized stories from beta sellers about chargebacks won, scams blocked, defects avoided. Run on the blog and link from pricing.

**Definition of done:**

- All four sub-features behind a feature flag (`seller_shield_enabled`) gated to **Growth tier and above** ($49+).
- ≥10 paying customers have at least one of the four enabled in production.
- ≥1 documented chargeback win using the auto-assembled packet (this is the case study).
- Pack-out evidence has been auto-attached to at least one real dispute.

**Target by end of month 8:** 180 paying customers, ~$6.5k MRR; Growth-tier upgrades visibly accelerated by Seller Shield availability.

---

## Year 1, second half (month 9–12)

With Seller Shield shipped, the remaining year-1 work focuses on integrations that unlock new customer segments (accountants, EU sellers, multi-marketplace sellers).

| Capability                                                | Where it lives                                          | Target month |
|----------------------------------------------------------|---------------------------------------------------------|--------------|
| **QuickBooks Online direct sync**                         | new `accounting-api` endpoint + outbound integration    | M9           |
| **Xero direct sync**                                      | new `accounting-api` endpoint                            | M9           |
| **"Caught eBay's mistake" auditor** (IDEAS.md #18)       | new worker inside `accounting-api` (no new service)     | M9           |
| **Replacement-instead-of-refund flow** (FEATURES 12.5)   | `sync-api` extension                                     | M10          |
| **Defective-claim abuse tracker (single-tenant)** (FEATURES 12.7) | `analytics-api` extension                       | M10          |
| **Team roles + per-account permissions**                  | `auth-api` extension                                     | M10          |
| **VAT / OSS auto-return** for EU sellers (IDEAS.md #5)   | new `tax-api` service (split from `accounting-api`)     | M11          |
| **Amazon SP-API connector**                               | new `amazon-conn-api` service (sibling of `ebay-conn-api`) — proves the multi-marketplace pattern | M11 |
| **Atomic cross-marketplace inventory** (FEATURES 6.6)    | `inventory-api` extension — only meaningful once `amazon-conn-api` lands | M11–M12 |
| **Sourcing radar** (IDEAS.md #4)                          | new `ml-api` endpoint                                    | M12          |
| **White-label PDF reports**                               | `accounting-api`                                         | M12          |
| **Public REST API (read-only)**                           | exposed at `api-gateway` with separate API-key auth in `auth-api` | M12 |

By end of month 12: ~350 paying customers, ~$13k MRR, public API in beta, **Seller Shield in production for 6 months** with documented chargeback-recovery dollars saved. **14 services** in production (added `tax-api` and `amazon-conn-api` — Seller Shield added zero new services).

---

## Year 2 highlights (no detailed schedule)

- `etsy-conn-api`, `shopify-conn-api`, `walmart-conn-api` — same shape as `amazon-conn-api`, plug-and-play once the connector pattern is proven
- `agency-api` for the white-label / multi-tenant-view tier
- **Cross-tenant defective-claim network defense** (FEATURES 12.6, IDEAS #22) — once we have enough opted-in sellers for the signal to be meaningful, lift the single-tenant tracker (12.7) into a cross-tenant, hashed-buyer-ID network in `analytics-api`. Privacy-defensible per [LEGAL.md](LEGAL.md).
- Inventory financing partnership (IDEAS.md #9) — new `financing-api` thin service
- "Replay & rewind" simulator (IDEAS.md #10) — `analytics-api` extension
- Native mobile (only if PWA data justifies it)
- SOC 2 Type II (if enterprise contracts demand)
- Move from Docker Compose → k3s when service count crosses ~18 or traffic crosses one box's headroom

---

## What we're explicitly NOT doing in year 1

- **A separate Postgres cluster per service.** One cluster, schema-per-service is enough through year 1. Splits happen only when a service's load forces it.
- **A message broker beyond Postgres LISTEN/NOTIFY.** No Kafka, no RabbitMQ, no NATS until we have >3 consumers per topic or need event replay. Then NATS JetStream.
- **Service mesh** (Istio / Linkerd). Resilience4j + OTel cover us at this scale.
- **Kubernetes.** Docker Compose on the AX102 is enough through year 2.
- **Splitting a service that hasn't outgrown its module.** No "microservice for the sake of microservices" beyond the 14 we've planned.
- A separate listing-creator app. eBay's is fine.
- Becoming a payment processor. Stripe is for that.
- Multi-region deploys. EU box is enough.

---

## How we'll know we're winning

Business metrics:

| Metric                                      | M3   | M6   | M8 (Seller Shield) | M12  |
|---------------------------------------------|------|------|--------------------|------|
| Free signups                                | 200  | 1,200| 2,500              | 6,000|
| Paying customers                            | 15   | 100  | 180                | 350  |
| MRR ($)                                     | 570  | 3,200| 6,500              | 13,300|
| Net revenue retention (NRR)                 | n/a  | >100%| >105%              | >115%|
| Activation rate (signup → connected eBay)   | 60%  | 70%  | 75%                | 80%  |
| Conversion (trial → paid)                   | 25%  | 30%  | 32%                | 35%  |
| Monthly churn                               | <5%  | <4%  | <3.5%              | <3%  |
| NPS                                         | n/a  | >40  | >45                | >50  |

Seller Shield-specific metrics (from M8 onwards):

| Metric                                                       | M8     | M12    |
|--------------------------------------------------------------|--------|--------|
| % of paying customers with at least one Shield feature enabled | >40% | >70%   |
| Chargeback cases handled via auto-assembled packet            | ≥10    | ≥80    |
| Documented chargeback wins ($ recovered, cumulative)          | ≥$2k   | ≥$25k  |
| Pack-out videos auto-attached to disputes                     | ≥20    | ≥200   |
| Address-change interceptions per month                        | ≥30    | ≥150   |
| 🔴 high-risk-order alerts fired per month                     | tracked| tracked|
| Reduction in seller-reported defect rate (Shield vs no-Shield) | TBD   | ≥30%   |

Engineering / ops metrics specific to the microservices split:

| Metric                                                       | M3        | M6        | M12       |
|--------------------------------------------------------------|-----------|-----------|-----------|
| Services in production                                       | 4         | 11 + ml-api| 14       |
| Independent deploys per week (across all services)           | 5         | 15        | 30        |
| p95 inter-service call latency (in-DC)                       | <50ms     | <30ms     | <25ms     |
| Contract-test coverage (consumer/provider pairs)             | 100% of pairs | 100%  | 100%      |
| Time from green CI → production for a single-service change  | <15 min   | <10 min   | <8 min    |
| Mean time to recovery (MTTR) for one-service outage          | <30 min   | <15 min   | <10 min   |
| % of incidents traced to a missing/broken contract           | n/a       | 0         | 0         |
| Cross-schema queries detected by CI                          | 0         | 0         | 0         |

If business MRR misses by 2× by M6 — pause, talk to 30 customers, possibly pivot pricing or focus. Don't keep building.

If engineering metrics start drifting — inter-service latency creeping up, deploys slowing, contract tests breaking weekly — that's the signal that we've over-split or under-invested in shared libs. Consolidate before adding more services.
