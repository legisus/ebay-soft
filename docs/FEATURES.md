# Features

Features grouped by the user problem they solve, with **stage** tags and the **service(s) that own them**. The service split is defined in [ARCHITECTURE.md](ARCHITECTURE.md); each feature row points at which service stores the data, which service exposes the API, and which (if any) services consume events to react.

Conventions used in the tables below:

- **Owner** — the service that holds the data and the authoritative API for this feature.
- **Consumers** — services that react via events or call the owner's REST API.
- **Stage** — one of:
  - **MVP** — shipped by [ROADMAP.md](ROADMAP.md) Phase 7 (public launch, month 6).
  - **P2** — *post-MVP wave 1*. Shipped between [ROADMAP.md](ROADMAP.md) Phase 8 and end of year 1 (months 7–12). **Note**: "P2" is a staging label here, not the same as "ROADMAP Phase 2" — ROADMAP Phase 2 is part of the MVP build. Always look up the exact ROADMAP location when timing matters.
  - **P3** — *post-MVP wave 2*. Year 2+ work; see ROADMAP's "Year 2 highlights" section.

A feature never spans two owners. If the table lists more than one service for a feature, exactly one is the owner and the rest are consumers.

---

## 1. Accounting & finance

| # | Feature                                            | Stage | Owner             | Consumers / sources                                  |
|---|----------------------------------------------------|-------|-------------------|------------------------------------------------------|
| 1.1 | True net-income P&L (per day/listing/category)   | MVP   | `accounting-api`  | reads orders/finance events via `sync-api` events    |
| 1.2 | Fee reconciliation, every fee tied to its order  | MVP   | `accounting-api`  | data from `sync-api`                                 |
| 1.3 | CSV / XLSX export (QuickBooks / Xero compatible) | MVP   | `accounting-api`  | objects stored in MinIO, URL signed                  |
| 1.4 | PDF monthly P&L / cash-flow / tax summary        | MVP   | `accounting-api`  | MinIO                                                |
| 1.5 | QuickBooks Online direct sync                    | P2    | `accounting-api`  | outbound integration; OAuth token stored in own schema |
| 1.6 | Xero direct sync                                 | P2    | `accounting-api`  | same shape as QBO                                    |
| 1.7 | VAT / sales-tax reports (OSS / IOSS / US states) | P2    | **`tax-api`** (split off in year 1 H2) | reads from `accounting-api` + `inventory-api` via APIs |
| 1.8 | Multi-currency, base currency per tenant         | MVP   | `accounting-api`  | uses `fx_rates` (see [DATABASE.md](DATABASE.md)); `Money` type carries currency end-to-end |
| 1.9 | Cash-flow forecast next 60 days                  | P3    | `accounting-api`  | calls `ml-api` for the forecast model                |
| 1.10 | Profitability waterfall chart                   | P3    | `accounting-api`  | data endpoint; viz in SPA                            |
| 1.11 | **"Caught eBay's mistake" auditor** (IDEAS #18) | P2    | `accounting-api`  | runs as a worker inside the service                  |

---

## 2. Inventory & stock management

| #   | Feature                                          | Stage | Owner             | Consumers / sources                                |
|-----|--------------------------------------------------|-------|-------------------|----------------------------------------------------|
| 2.1 | SKU master (one SKU → many listings)             | MVP   | `inventory-api`   | `accounting-api` reads SKUs via API for COGS lookup |
| 2.2 | Multi-warehouse stock with reserved/available    | MVP   | `inventory-api`   | —                                                  |
| 2.3 | Low-stock alerts                                 | MVP   | `inventory-api` (publishes `stock.low`) | `notif-api` consumes the event |
| 2.4 | Bulk CSV import / export of SKUs                 | MVP   | `inventory-api`   | —                                                  |
| 2.5 | Auto-relist on back-in-stock                     | P2    | `inventory-api` (publishes `stock.restocked`) | `ebay-conn-api` performs the relist via eBay API |
| 2.6 | Restock lead-time tracker                        | P2    | `inventory-api`   | —                                                  |
| 2.7 | Bundle / kit SKUs                                | P2    | `inventory-api`   | —                                                  |
| 2.8 | Purchase orders module                           | P3    | `inventory-api` (new aggregate inside it) | —                            |
| 2.9 | Barcode / GS1 support                            | P3    | `inventory-api`   | —                                                  |

> Migration note: SKU master temporarily lives in `accounting-api` during Phase 2 of the roadmap, then moves to `inventory-api` in Phase 3 with a documented data cutover. See [ROADMAP.md](ROADMAP.md).

---

## 3. ML analytics & insights

| #   | Feature                                          | Stage | Owner             | Consumers / sources                                |
|-----|--------------------------------------------------|-------|-------------------|----------------------------------------------------|
| 3.1 | Best-sellers (revenue / units / margin / STR)    | MVP   | `analytics-api`   | materializes from `order.synced` events            |
| 3.2 | Dead-stock report                                | MVP   | `analytics-api`   | joins data fetched from `inventory-api` via API    |
| 3.3 | Hourly / day-of-week heatmap                     | MVP   | `analytics-api`   | —                                                  |
| 3.4 | Category mix treemap                             | MVP   | `analytics-api`   | —                                                  |
| 3.5 | Demand forecast per SKU                          | P2    | `ml-api`          | scheduled job in `sync-api` triggers retraining nightly |
| 3.6 | Price-elasticity estimate                        | P2    | `ml-api`          | consumed by `repricer-api` for "suggested price"   |
| 3.7 | **"Why this listing isn't selling" diagnostic** (IDEAS #2) | P2 | `ml-api`     | called by SPA via gateway; data inputs from `sync-api` + `accounting-api` |
| 3.8 | Profit-vs-velocity quadrant                      | P3    | `analytics-api`   | clustering call to `ml-api`                        |
| 3.9 | Sourcing radar (IDEAS #4)                        | P3    | `ml-api`          | recommendations rendered in SPA                    |
| 3.10 | Promoted-Listings ROI scoreboard (IDEAS #7)     | P2    | `analytics-api`   | joins ad-spend from `sync-api` with revenue from `accounting-api` |

---

## 4. Listing optimizer

Listing-optimizer features live inside `sync-api` rather than getting their own service. `sync-api` already holds the listing data and has the eBay credentials it needs to read taxonomy and images; spinning up a "listing-optimizer-api" would be a microservice for the sake of one.

| #   | Feature                                          | Stage | Owner       | Consumers / sources                                |
|-----|--------------------------------------------------|-------|-------------|----------------------------------------------------|
| 4.1 | Title scorer                                     | MVP   | `sync-api`  | reads category taxonomy from eBay via `ebay-conn-api` |
| 4.2 | Image audit (count, resolution, white-bg)        | MVP   | `sync-api`  | small ONNX model in-process                        |
| 4.3 | Aspect completeness                              | MVP   | `sync-api`  | eBay taxonomy cache                                |
| 4.4 | A/B test orchestrator                            | P2    | `sync-api`  | publishes `listing.variant_assigned` events; `analytics-api` aggregates winners |
| 4.5 | AI title rewrite (brand-voice aware, IDEAS #13)  | P2    | `sync-api`  | calls an LLM provider; brand-voice config in own schema |
| 4.6 | Image background remover                         | P2    | `sync-api`  | `rembg` ONNX model; uploads results to MinIO       |

---

## 5. Repricer

| #   | Feature                                          | Stage | Owner          | Consumers / sources                                |
|-----|--------------------------------------------------|-------|----------------|----------------------------------------------------|
| 5.1 | Rule-based engine                                | MVP   | `repricer-api` | scheduled worker pushes price changes via `ebay-conn-api` proxy (only that service holds eBay tokens) |
| 5.2 | **Margin floor** safety (IDEAS #3)               | MVP   | `repricer-api` | pulls margin via `accounting-api` REST call before every reprice |
| 5.3 | Dry-run mode                                     | MVP   | `repricer-api` | logs proposed changes; no eBay write              |
| 5.4 | Competitor watch                                 | P2    | `repricer-api` | Browse API access proxied through `ebay-conn-api` |
| 5.5 | ML-suggested optimal price                       | P2    | `repricer-api` | calls `ml-api` for elasticity-based recommendation |

---

## 6. Multi-account / multi-marketplace

| #   | Feature                                          | Stage | Owner                                       | Consumers / sources                                |
|-----|--------------------------------------------------|-------|---------------------------------------------|----------------------------------------------------|
| 6.1 | One tenant → many eBay accounts                  | MVP   | `ebay-conn-api`                             | model already supports it (see [DATABASE.md](DATABASE.md)) |
| 6.2 | Amazon Seller Central (SP-API)                   | P2    | **`amazon-conn-api`** (new sibling service) | publishes the same event shapes as `ebay-conn-api`; `sync-api`, `accounting-api`, `inventory-api` consume them generically |
| 6.3 | Etsy                                             | P3    | **`etsy-conn-api`**                          | same pattern                                       |
| 6.4 | Shopify                                          | P3    | **`shopify-conn-api`**                       | same pattern                                       |
| 6.5 | Walmart Marketplace, Mercari                     | P3    | per-marketplace `*-conn-api` services        | same pattern                                       |
| 6.6 | **Atomic cross-marketplace inventory** — sell once, decrement everywhere within ~5s, auto-delist at zero (prevents the "double sale" forum sellers describe as their worst nightmare) | P2 | `inventory-api` (publishes `stock.decremented`) | every `*-conn-api` consumes and updates its marketplace listings via the connector's write APIs |
| 6.7 | Cross-marketplace **Item Specifics ↔ category mapping** — eBay aspects (Brand/Color/Size/Material per category) auto-mapped to Poshmark categories and Mercari attributes | P2 | `sync-api`         | trained mapping table seeded from eBay taxonomy; per-listing override allowed |

The connector pattern is proven once with Amazon; each new marketplace is a copy of that template — sync/accounting/inventory remain unchanged. Atomic inventory (6.6) is what makes the multi-marketplace story real for sellers — without it, multi-channel is a liability, not a feature.

---

## 7. Tax & compliance

| #   | Feature                                          | Stage | Owner             | Consumers / sources                                |
|-----|--------------------------------------------------|-------|-------------------|----------------------------------------------------|
| 7.1 | VAT MOSS / OSS report (EU)                       | P2    | `tax-api`         | reads from `accounting-api` + `inventory-api` via REST |
| 7.2 | IOSS for imports ≤ €150 (EU regulatory threshold, denominated in EUR by law) | P2    | `tax-api`         | —                                  |
| 7.3 | EPR marker per country (FR, DE)                  | P2    | `tax-api`         | —                                                  |
| 7.4 | US sales-tax marketplace-facilitator reconciliation | P2 | `accounting-api`  | reads facilitator events from `sync-api`           |
| 7.5 | 1099-K reconciliation (US)                       | P2    | `accounting-api`  | —                                                  |
| 7.6 | Marketplace Account Deletion notification        | MVP (required by eBay) | `ebay-conn-api` | hard-deletes PII; emits `tenant.gdpr_purge` for other services — protocol details in [EBAY_API.md](EBAY_API.md) |

---

## 8. Notifications & automation

| #   | Feature                                          | Stage | Owner       | Consumers / sources                                |
|-----|--------------------------------------------------|-------|-------------|----------------------------------------------------|
| 8.1 | Email + in-app for core events                   | MVP   | `notif-api` | consumes `stock.low`, `ebay_account.expired`, `payout.received`, `dispute.opened`, `subscription.changed` |
| 8.2 | Telegram bot                                     | P2    | `notif-api` | new outbound channel                               |
| 8.3 | Slack webhook                                    | P2    | `notif-api` | new outbound channel                               |
| 8.4 | Custom alerts ("SKU X drops below 5")            | P2    | `notif-api` | rule engine + per-tenant rules in its own schema   |
| 8.5 | **Daily morning brief** (Telegram/Slack, IDEAS #16) | P2 | `notif-api` | scheduled job; pulls metrics from `analytics-api`  |
| 8.6 | Public outbound webhooks (`sale.created` etc.)   | P3    | `notif-api` | retry-with-backoff, signing keys                   |

---

## 9. Multi-user / team

| #   | Feature                                          | Stage | Owner       | Consumers / sources                                |
|-----|--------------------------------------------------|-------|-------------|----------------------------------------------------|
| 9.1 | Roles: owner / member / viewer                   | P2    | `auth-api`  | role claim on JWT; every service trusts it         |
| 9.2 | Per-eBay-account permissions                     | P2    | `auth-api`  | `ebay-conn-api` validates the claim before exposing an account |
| 9.3 | Audit log visible to owner                       | P2    | `admin-api` | aggregated view; reads `audit_log` from each service via its public API |

Note: every service writes to its own `audit_log` table; `admin-api` aggregates a tenant's view across services.

---

## 10. API & integrations

| #    | Feature                                          | Stage | Owner            | Consumers / sources                                |
|------|--------------------------------------------------|-------|------------------|----------------------------------------------------|
| 10.1 | Public REST API (read-only)                      | P3    | `api-gateway` + `auth-api` (API-key auth) | routes to existing services; no new APIs invented |
| 10.2 | Public REST API (write endpoints)                | P3    | same             | per-service rate limits, idempotency keys           |
| 10.3 | Zapier / Make.com apps                           | P3    | none (use public API) | thin Zapier package; docs only                 |
| 10.4 | n8n template pack                                | P3    | none             | docs only                                          |
| 10.5 | Outbound webhook subscriptions                   | P3    | `notif-api`      | see 8.6                                            |

---

## 11. Mobile

| #    | Feature                                          | Stage | Owner       | Consumers / sources                                |
|------|--------------------------------------------------|-------|-------------|----------------------------------------------------|
| 11.1 | Read-only PWA (works on phones)                  | MVP   | SPA (no service) | uses the same gateway endpoints                |
| 11.2 | Native iOS / Android                             | P3    | new mobile clients | gated on >40% mobile DAU; uses public API     |

---

## 12. Fraud & risk defense

Derived from forum-validated seller pain points (see [IDEAS.md](IDEAS.md) #19–#22). These features defend a seller's revenue, defect rate, and time — the three things eBay's native platform protects the worst.

| #    | Feature                                          | Stage | Owner             | Consumers / sources                                |
|------|--------------------------------------------------|-------|-------------------|----------------------------------------------------|
| 12.1 | **Buyer-risk score per order** (🟢/🟡/🔴 from feedback age, dispute history, return-rate, account age, velocity) | P2 | `analytics-api` (computes) | `sync-api` provides inputs; `notif-api` raises alert on 🔴 |
| 12.2 | **Address-change interceptor** — flag and hold orders where buyer asks to ship to a different address than the order; warn that this voids eBay seller protection | P2 | `sync-api` (NLP on message threads + change detection) | `notif-api` emails the seller; UI shows the warning banner |
| 12.3 | **Pack-out evidence capture** — timestamped video / photo workflow with order-ID overlay, stored as immutable evidence; auto-attached to disputes | P2 | `inventory-api` (orchestrates the workflow at packout) | MinIO for storage; `notif-api` notifies on dispute auto-attach |
| 12.4 | **Bank-chargeback defense workflow** — detect chargeback webhooks, auto-assemble representment evidence packet per card network, pre-fill bank template | P2 | `billing-api` (orchestrator) | pulls tracking from `sync-api`, listing photos via `ebay-conn-api`, pack-out video from MinIO, P&L context from `accounting-api` |
| 12.5 | **Replacement-instead-of-refund flow** — orchestrate "send replacement" when an order goes wrong, instead of forcing the buyer through eBay's native refund | P3 | `sync-api`         | uses eBay Messaging API; emits `order.replaced` event; `inventory-api` decrements stock; `accounting-api` records the net |
| 12.6 | **Cross-tenant defective-claim network defense** — opt-in, hashed-buyer-ID network signal: "this buyer filed N INAD claims across M sellers in last 12 months" | P3 | `analytics-api` (cross-tenant aggregation, strict opt-in, anonymized) | feeds into 12.1's buyer-risk score |
| 12.7 | **Defective-claim abuse tracker (single-tenant first)** — flag buyers whose history with *this* seller shows serial "item not as described" patterns | P2 | `analytics-api`    | sets up the data shape for 12.6 later                |

12.1–12.4 ship together as a coherent **"Seller Shield"** feature in [ROADMAP.md](ROADMAP.md) **Phase 8** (months 7–8, immediately after public launch) — the screenshot is the 🟢/🟡/🔴 badge on the orders page with the address-change banner above. 12.5 (replacement flow) and 12.7 (single-tenant defective-claim tracker) follow in year 1 H2 around M10; 12.6 (cross-tenant defective-claim network) is gated on having enough opted-in sellers to make the signal meaningful — slated for Year 2.

---

## 13. Cross-cutting capabilities (not user-visible but underpin features)

| Capability                                       | Owner / location                                    |
|--------------------------------------------------|------------------------------------------------------|
| `Money` value type + serialization               | `libs/common-domain` (used by every service handling amounts) |
| JWT issuance + JWKS                              | `auth-api` (every service validates)                |
| Service-to-service auth                          | `libs/common-security` (internal JWT)               |
| OpenTelemetry tracing                            | every service via `libs/common-web`                 |
| Outbox event publishing                          | every publishing service via `libs/common-events`   |
| Generated OpenAPI clients                        | one per service in `clients/*-api-client`           |
| Plan-limit enforcement                           | `billing-api` publishes `subscription.changed`; `auth-api` refreshes plan claim on JWT |
| Stripe Tax integration                           | `billing-api`                                       |
| File storage (PDFs, CSV exports, removed-bg images) | MinIO; signed URLs minted by each service's API |

---

## MVP scope summary (3-month target)

To launch publicly, ship exactly this set of features across exactly these services. Everything else waits.

| MVP feature                                            | Services that ship it                                  |
|--------------------------------------------------------|--------------------------------------------------------|
| Sign-up, login, 2FA, tenants, users                    | `auth-api`                                             |
| Routing, JWT validation, rate limit, OTel root span    | `api-gateway`                                          |
| eBay OAuth + token storage + Marketplace-Deletion endpoint | `ebay-conn-api`                                    |
| Backfill + incremental sync (orders, listings, finances) | `sync-api`                                           |
| P&L (per day / month / year / listing / category)      | `accounting-api`                                       |
| CSV / XLSX / PDF export                                | `accounting-api` → MinIO                               |
| Dashboard widgets, top sellers, dead-stock, heatmap    | `analytics-api`                                        |
| **Profit Mode toggle** (IDEAS #11)                     | SPA (data already separated by `accounting-api`)       |
| SKU master + low-stock alerts                          | `inventory-api` + `notif-api` (event consumer)         |
| Email + in-app notifications                           | `notif-api`                                            |
| Stripe Checkout + plan limits + 14-day Pro trial       | `billing-api`                                          |
| Internal back-office                                   | `admin-api` (minimum: tenant view, impersonation)      |

**MVP service count: 8.** That's `api-gateway` + 7 backend services. No `repricer-api`, no `ml-api` at MVP. Ship → talk to users → build the next thing they actually pay for. The rest follows the schedule in [ROADMAP.md](ROADMAP.md).
