# Monetization

## Pricing model

**Base currency is USD.** Subscription, monthly or yearly (yearly = 2 months free), priced in USD ($), EUR (€), and GBP (£) with parity at launch. Sales tax / VAT handled by Stripe Tax. Reporting in the app defaults to the tenant's selected display currency; underlying amounts always travel as `Money(amount, currency)` (see [BACKEND.md → Money handling](BACKEND.md#money-handling)).

| Tier         | Price ($/mo, monthly billing) | Orders/mo cap | eBay accounts | Channels | Key features |
|--------------|-------------------------------|---------------|---------------|----------|---------------|
| **Free**     | $0                            | 50            | 1             | eBay only | P&L lite (30-day), CSV export, no PDF, no repricer |
| **Starter**  | $19                           | 500           | 1             | eBay only | Full P&L, all export formats, low-stock alerts, dead-stock report, QuickBooks/Xero one-way push |
| **Growth**   | $49                           | 5,000         | 3             | eBay + Amazon (Phase 2) | Repricer (rules), listing optimizer, ML best-seller insights, Promoted Listings ROI, Telegram bot |
| **Pro**      | $99                           | 25,000        | 10            | eBay + Amazon + Etsy + Shopify | Demand forecast, price-elasticity, "Why isn't this selling" diagnostic, A/B title testing, custom alerts, public API (read) |
| **Scale**    | $249                          | 100,000       | unlimited     | all       | White-label PDFs, full team roles, public API (write), priority support, dedicated Slack |
| **Agency**   | Custom (from $499)            | unlimited     | unlimited     | all       | Master view across many tenants, white-label, single bill |

EUR pricing: €19 / €49 / €99 / €249 (parity). GBP: £17 / £42 / £85 / £215.

## Why this shape works

Pricing is set against the incumbent landscape — see [COMPETITORS.md](COMPETITORS.md) for the benchmarks each tier is positioned against.

- **Free tier with 50 orders** lets a hobby seller actually feel the product. They're not the buyer but they tell others. Costs us almost nothing (the bottleneck is API quota, which is per-app not per-seller).
- **Starter at $19** sits right below A2X's $29 entry, on purpose. We undercut the bookkeeping bridge and **offer more**.
- **Growth at $49** is where the repricer + analytics kick in — that's the upgrade hook, and it sits below Sellbrite mid-tier.
- **Pro at $99** matches Sellbrite/Vendio mid pricing while being meaningfully more capable.
- **Scale at $249** is for serious operators; cheaper than Linnworks by an order of magnitude.

## Trial

- 14-day **full Pro** trial on signup, no card required.
- After trial, the account auto-downgrades to Free. We don't dark-pattern.

## Add-ons (à la carte)

| Add-on                            | Price         | Notes |
|-----------------------------------|---------------|-------|
| Extra 5,000 orders/mo             | $15           | Stacks |
| Extra eBay account                | $9 / account  | Stacks |
| White-label PDFs                  | Included Scale+ | — |
| Dedicated server / VPN            | Custom        | Phase 3, enterprise only |

## Discounts

- **Annual** — 2 months free (= 16% off).
- **Non-profit / education** — 50% off Pro.
- **Migration discount** — 3 months free Growth if migrating from A2X / Link My Books / Sellbrite, on production of an invoice from them.

## Revenue projections (sanity check, not a forecast)

| Month | Free signups | Paying customers (avg $38/mo blended) | MRR        | Notes                                    |
|-------|--------------|---------------------------------------|------------|------------------------------------------|
| M3 (launch) | 200      | 15                                    | $570       | Friends & beta                           |
| M6    | 1,200        | 80                                    | $3,040     | First SEO traffic from free tools        |
| M12   | 6,000        | 350                                   | $13,300    | Annual plans kick in, agency channel     |
| M18   | 15,000       | 900                                   | $34,200    | Amazon + Etsy launched                   |
| M24   | 30,000       | 2,000                                 | $76,000    | First serious agency contracts           |

This is the **conservative** path. Aggressive case ~2.5× if a paid-acquisition channel works.

## Costs (per-month, M12 scenario)

All amounts in USD. Hetzner publishes prices in EUR; the USD figures below assume ~1.08 EUR/USD and round up.

| Item                          | Cost/mo  |
|-------------------------------|----------|
| Hetzner AX102 (16-core, 128GB) — billed ~€130 | $140     |
| Hetzner Storage Box 5TB — billed ~€13         | $14      |
| Cloudflare Pro                | $20      |
| Stripe fees (~3%)             | ~$400    |
| Email (Postmark / Resend)     | $40      |
| **Twilio Verify (SMS OTP)**, ~5k verifications/mo at $0.05 avg | $250 |
| LLM API for AI features       | $200     |
| Monitoring (Grafana Cloud free, Sentry Team) | $30 |
| Domain, misc.                 | $10      |
| **Total infra/services**      | **~$1,100** |

Gross margin at M12 ≈ (13,300 − 1,100) / 13,300 ≈ **92%**. Headcount is the other side; one founder + one contractor at M12 keeps us cash-positive even early.

Variable-cost watch: Twilio SMS is the only line item that scales with *user count* rather than tenant count, so abusive sign-ups (free-tier accounts created just to burn OTPs) could spike it. Mitigations are codified in [SECURITY.md → Authentication](SECURITY.md#authentication): per-phone rate limits, country gates, paid-plan requirement for high-risk destinations, and CAPTCHA on the sign-up flow.

## Payment processing

- Stripe Checkout + Customer Portal — no custom payment UI at MVP.
- Stripe Tax — VAT / sales tax handled automatically.
- Invoice download via portal.
- Failed payment → 3 retries (Stripe Smart Retries) → email → downgrade after 7 days.

## Refund policy

- 30-day money-back, no questions.
- Self-serve cancellation. We don't make people email to cancel — that's the #1 complaint about A2X.

## Future revenue paths (beyond subscription)

1. **Inventory financing referral fee** — see [IDEAS.md](IDEAS.md) #9.
2. **Accountant marketplace** — vetted accountants pay to be listed; sellers pay to be matched.
3. **Marketplace insights data product** — anonymized aggregate data sold to brands / sourcing agents (consent-gated).
4. **Affiliate revenue** — eBay Partner Network (if approved), shipping platforms (ShipStation, Pirate Ship).
5. **One-time setup / migration service** — $500–$2,000 per seller, useful at Scale+ tier.
