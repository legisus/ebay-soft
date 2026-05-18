# ebay-soft

SaaS platform for eBay sellers: accounting, inventory, analytics, and ML-driven profit insights.

**Domain:** ebay-soft.com
**Hosting:** Hetzner dedicated server
**Status:** Pre-development / planning

---

## The pitch

ebay-soft is the dashboard eBay should have built: a true net-margin view of every listing, ML that tells you which inventory to buy and which to dump, a repricer that refuses to lose money for you, automatic accountant-ready reports, and **Seller Shield** — the defense pack that catches scams, fights chargebacks, and proves what you shipped.

Three brand-defining moments to lead marketing with:

1. **Profit Black Box** — sellers see real margin per listing for the first time
2. **"Why isn't this selling?" diagnostic** — structured, actionable per-listing report
3. **Seller Shield** — defends a seller's account against the scams every forum thread complains about

Plus the retention killer: **"We caught eBay's mistake" auditor** — returns ~$200/month to a typical seller in errors eBay quietly made.

---

## What sellers can do

Stage tags: **MVP** = public launch at month 6, **Shield** = Phase 8 (months 7–8), **Y1** = months 9–12, **Y2** = year 2+. Full catalogue with service ownership in [docs/FEATURES.md](docs/FEATURES.md); differentiator rationale in [docs/IDEAS.md](docs/IDEAS.md); phased delivery in [docs/ROADMAP.md](docs/ROADMAP.md).

### See your real money

- True net-income P&L by day / listing / category, with **Profit Mode** toggle on every chart (MVP)
- **Profit Black Box** waterfall per listing — sale → fee → ad → refund → COGS → net (MVP)
- Fee reconciliation tied back to source orders (MVP)
- CSV / XLSX / PDF exports for accountants (MVP)
- **"Caught eBay's mistake" auditor** — flags duplicate fees, unreturned refunds, payout mismatches in your favor (Y1)
- QuickBooks / Xero direct sync (Y1)
- Cash-flow forecast (Y2)

### Know your inventory

- SKU master + COGS, multi-warehouse stock, low-stock alerts, dead-stock report (MVP)
- **Atomic cross-marketplace inventory** — sell once, decrement everywhere within 5s, auto-delist at zero (Y1)
- Restock lead-time tracker, bundle / kit SKUs (Y1)

### Sell smarter (ML insights)

- Best-sellers by revenue / units / **net margin** / sell-through; hourly heatmap; category treemap (MVP)
- **"Why isn't this selling?" diagnostic** — title, aspects, images, price percentile, shipping ratio, age (month 4)
- Demand forecast and price-elasticity per SKU (month 4)
- **Promoted-Listings counterfactual ROI** — true incremental margin, not gross attribution (month 4)
- Sourcing radar, profit-vs-velocity quadrant, replay simulator (Y1–Y2)

### Better listings

- Title scorer, image audit, aspect completeness (month 5)
- AI title rewrite with **brand-voice config** (month 5)
- A/B testing, image background remover, cross-marketplace category mapping (Y1)

### Win the price game

- Rule-based repricer with **margin-floor safety** — refuses to drop below `(COGS + fees) × (1 + min_margin%)` (month 5)
- Dry-run mode (month 5)
- Competitor watch, ML-suggested optimal price (Y1)

### Sell on more places

- Multiple eBay accounts per tenant (MVP)
- Amazon Seller Central via SP-API (Y1)
- Etsy, Shopify, Walmart, Mercari (Y2)
- White-label PDF reports for agencies (Y1)

### Tax & compliance

- EU VAT **OSS / IOSS** auto-return via new `tax-api` (Y1)
- US 1099-K reconciliation, marketplace-facilitator sales-tax (Y1)
- EPR markers for FR / DE (Y1)
- Marketplace Account Deletion endpoint (MVP — eBay requires it)

### Notifications & automation

- Email + in-app alerts: low stock, OAuth expiry, dispute opened, payout received, weekly summary (MVP)
- **Telegram daily brief** — yesterday's revenue, net margin, top SKU, low-stock, new disputes (launch)
- Slack webhook, custom alerts (Y1)
- Outbound webhooks for the seller's own systems (Y2)

### Seller Shield — the differentiator pack

The 2026-05 forum research said this was the unmet need. Ships in Phase 8, right after public launch.

- **🟢 / 🟡 / 🔴 buyer-risk score** on every order (Shield)
- **Address-change interceptor** — auto-holds orders where the buyer asks to ship elsewhere, with a banner explaining how that voids eBay protection (Shield)
- **Pack-out evidence capture** — phone-based timestamped video with order-ID overlay, immutable in MinIO, auto-attached to any future dispute (Shield)
- **Bank-chargeback workflow** — Stripe webhook → auto-assembled per-network representment packet (tracking + listing photos + pack-out video + buyer history + eBay dispute outcome). A single won chargeback pays for a year (Shield)
- Replacement-instead-of-refund flow (Y1)
- Single-tenant defective-claim tracker (Y1)
- Cross-tenant defective-claim network defense — opt-in, hashed buyer IDs (Y2)

### Team, API, mobile

- Roles (owner / member / viewer), per-account permissions, audit log (Y1)
- Public REST API read-only (Y1), write endpoints + Zapier / Make.com (Y2)
- Mobile **PWA** from day one (MVP); native iOS / Android only if PWA usage justifies (Y2)

---

## Pricing at a glance

Base currency **USD**; EUR and GBP at parity. Full rationale and tier details in [docs/MONETIZATION.md](docs/MONETIZATION.md).

| Tier         | $/mo | Fit |
|--------------|------|-----|
| **Free**     | $0   | Hobbyist, ≤50 orders/mo — 30-day P&L only, no PDF, no repricer, no Shield |
| **Starter**  | $19  | ≤500 orders/mo — full P&L, all exports, low-stock alerts, one-way QuickBooks/Xero push |
| **Growth**   | $49  | ≤5,000 orders/mo, 3 eBay accounts — **Seller Shield**, repricer, ML best-sellers, Promoted-Listings ROI, Telegram brief |
| **Pro**      | $99  | ≤25k orders/mo, 10 accounts, multi-marketplace, demand forecast, elasticity, A/B testing, read-only API |
| **Scale**    | $249 | ≤100k orders/mo, unlimited accounts, white-label PDFs, team roles, write API, priority support |
| **Agency**   | from $499 | Master view across many tenants, white-label, single bill |

---

## Launch timeline at a glance

| When        | What ships |
|-------------|------------|
| Month 6     | **Public launch** — MVP capabilities + Telegram daily brief + 5 free SEO tools on marketing site + migration discount targeting A2X / LMB / Sellbrite users |
| Months 7–8  | **Seller Shield** — buyer-risk score, address-change interceptor, pack-out evidence, chargeback workflow |
| Months 9–10 | QuickBooks / Xero direct sync, "Caught eBay's mistake" auditor, replacement-instead-of-refund flow, team roles, defective-claim tracker (single-tenant) |
| Months 11–12| VAT / OSS auto-return (new `tax-api`), Amazon SP-API (new `amazon-conn-api`), atomic cross-marketplace inventory, sourcing radar, public read-only API |
| Year 2      | Etsy / Shopify / Walmart connectors, cross-tenant defective-claim network, agency tier, inventory-financing referral, replay simulator |

Fully sequenced plan in [docs/ROADMAP.md](docs/ROADMAP.md).

---

## Technology stack

| Layer        | Choice                                                           |
|--------------|------------------------------------------------------------------|
| Architecture | **Dedicated REST services** behind a Spring Cloud Gateway. Each service owns its schema and its OpenAPI contract. |
| Backend      | Java 25 LTS, Spring Boot 3.5+, `@Slf4j`. **Spring MVC + virtual threads** by default, **Spring WebFlux** only for fan-out/streaming services (`ebay-conn-api`, `sync-api`, `analytics-api`). |
| Persistence  | PostgreSQL 17 (one cluster, one **schema per service**), Redis (cache + rate-limit + token buckets), MinIO (S3-compatible) for invoices/exports |
| Inter-service| REST + JSON over generated OpenAPI clients; Postgres `LISTEN/NOTIFY` as event bus at MVP, NATS JetStream later |
| Auth         | `auth-api` issues JWTs; gateway validates and propagates `X-Tenant-Id` / `X-User-Id`. eBay OAuth lives in `ebay-conn-api`. |
| Frontend     | React 19 + TypeScript + Vite, Apache ECharts — see [docs/FRONTEND.md](docs/FRONTEND.md) |
| ML           | Python micro-service (FastAPI, `ml-api`) using scikit-learn / Prophet / LightGBM |
| Infra        | Hetzner dedicated server (AX-line), Docker Compose, Cloudflare in front; k3s only when scale forces it |
| Observability| OpenTelemetry → Tempo, Prometheus + Grafana, Loki, Sentry for errors |

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the full service list and [docs/BACKEND.md](docs/BACKEND.md) for build/deploy details.

---

## Documentation

Grouped by topic; within each group the files are listed in reading order.

### Product & strategy

| File                                          | Purpose                                                  |
|-----------------------------------------------|----------------------------------------------------------|
| [docs/FEATURES.md](docs/FEATURES.md)          | Feature list, mapped to the service that owns each — MVP / Phase 2 / Phase 3 |
| [docs/IDEAS.md](docs/IDEAS.md)                | Differentiation ideas — what makes ebay-soft genuinely interesting vs. incumbents |
| [docs/COMPETITORS.md](docs/COMPETITORS.md)    | A2X, Link My Books, Sellbrite, Linnworks et al. — where we win, where we lose |
| [docs/MONETIZATION.md](docs/MONETIZATION.md)  | Subscription tiers, trial, add-ons, MRR projections      |
| [docs/ROADMAP.md](docs/ROADMAP.md)            | Phased path from empty repo to public launch and year 1 |

### Engineering

| File                                          | Purpose                                                  |
|-----------------------------------------------|----------------------------------------------------------|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)  | Service list, gateway, data ownership, inter-service patterns |
| [docs/BACKEND.md](docs/BACKEND.md)            | Java 25 + Spring Boot per service, monorepo layout, `Money` handling |
| [docs/FRONTEND.md](docs/FRONTEND.md)          | React 19 + TypeScript + ECharts; chart-library comparison |
| [docs/DATABASE.md](docs/DATABASE.md)          | PostgreSQL schema per service, `Money` columns, RLS, migrations |
| [docs/EBAY_API.md](docs/EBAY_API.md)          | eBay REST surfaces, OAuth flow, rate limits, mandatory endpoints |
| [docs/API_DOCS.md](docs/API_DOCS.md)          | OpenAPI 3.1 + Swagger UI + Redoc developer portal, client generation, versioning |

### Operations

| File                                          | Purpose                                                  |
|-----------------------------------------------|----------------------------------------------------------|
| [docs/INFRASTRUCTURE.md](docs/INFRASTRUCTURE.md) | Hetzner box choice, container topology, monitoring, backups |
| [docs/ENVIRONMENTS.md](docs/ENVIRONMENTS.md)  | dev / stg / prod, CI/CD on GitHub Actions, version control, project board |
| [docs/OBSERVABILITY.md](docs/OBSERVABILITY.md) | Prometheus + Grafana + Loki + Tempo + Sentry, dashboards, alerting, on-call |
| [docs/TESTING.md](docs/TESTING.md)            | Test pyramid, frameworks per layer, contract testing, ephemeral E2E envs, CI gates |
| [docs/SECURITY.md](docs/SECURITY.md)          | Token encryption, RLS, secrets, incident response (technical side) |

### Business, legal & governance

| File                                          | Purpose                                                  |
|-----------------------------------------------|----------------------------------------------------------|
| [docs/GOVERNANCE.md](docs/GOVERNANCE.md)      | Open-source strategy, BSL license, donations vs subscriptions, hiring curve, trademark |
| [docs/LEGAL.md](docs/LEGAL.md)                | ToS, Privacy, Cookies (EU/US), DPA, why we need user data, sub-processor list |
