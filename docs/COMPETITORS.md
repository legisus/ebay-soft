# Competitor analysis

This is the landscape we're entering. The good news: it's a real market with real spend. The bad news: established incumbents own the high end. Our wedge has to be sharper than "we do the same thing, cheaper."

## Direct competitors (eBay-focused or with strong eBay support)

| Tool                  | Focus                                          | Pricing (Mar 2026 ballpark)         | Strengths                                                        | Weaknesses we can exploit                                                  |
|-----------------------|------------------------------------------------|--------------------------------------|------------------------------------------------------------------|----------------------------------------------------------------------------|
| **A2X for eBay**      | Accounting reconciliation → QuickBooks/Xero    | $19–$139/mo                          | Trusted by accountants; clean QB/Xero entries                    | Accounting-only; no inventory, no ML, no repricer; UI dated                |
| **Link My Books**     | Same: bookkeeping bridge eBay→Xero/QB          | $17–$99/mo                           | Excellent at fee reconciliation; UK-strong                       | Narrow scope; no analytics, no repricer                                    |
| **Sellbrite**         | Multi-channel listing + inventory              | $29–$249/mo                          | Mature, broad channel support, owned by GoDaddy                  | Weak P&L; analytics shallow; no ML; UI feels 2018                          |
| **InkFrog**           | Listing tool                                   | $11–$33/mo                           | Cheap, eBay-native                                               | Listing-only; no accounting; no analytics                                  |
| **3Dsellers**         | All-in-one toolkit                             | $20–$300/mo                          | Wide feature set; CRM; auto-message                              | Each module is thin; integration glue is loose                             |
| **CrazyLister**       | Listing templates                              | $25–$100/mo                          | Beautiful templates                                              | One-trick                                                                  |
| **Vendio**            | Multi-channel listing + inventory              | $30–$300/mo                          | Long history, supports niche channels                            | Stagnant product; weak analytics                                           |
| **SixBit**            | Desktop seller manager                         | $30–$60/mo                           | Power-user friendly for vintage / collectibles                   | Windows desktop only; no real cloud                                        |
| **GoDataFeed**        | Feed management                                | $79+/mo                              | Solid feed engine                                                | Not for solo sellers                                                       |
| **Auctiva**           | Listing + analytics                            | $10–$60/mo                           | Cheap, ad-supported variant                                      | Outdated, weak finances                                                    |
| **Linnworks**         | Order/inventory ops, enterprise multi-channel  | from ~$500/mo                        | Enterprise-grade ops; warehouse mgmt                             | Way too expensive and complex for small/medium sellers                     |
| **ChannelAdvisor / CommerceHub** | Enterprise multi-channel              | $1000s/mo                            | Big-retail features                                              | Out of reach for our ICP                                                   |
| **Ecomdash (Constant Contact)** | Inventory + listing                  | $50–$300/mo                          | Decent inventory                                                 | Discontinued for new customers in some markets — opportunity               |

## Adjacent / overlapping tools

| Tool                          | Why it touches us                                                |
|-------------------------------|------------------------------------------------------------------|
| **Helium 10**, **Jungle Scout** | Amazon analytics with eBay envy. The aesthetics and depth bar.   |
| **Keepa**                     | Amazon price history. Sets expectations for price charts.        |
| **Repricer.com**, **RepricerExpress** | Pure repricers; we compete on combined offering.         |
| **QuickBooks / Xero**         | Where the money actually flows; we integrate, not compete.       |
| **Shopify**                   | If a seller goes multi-channel beyond eBay, Shopify is the hub. We need to play nice. |
| **PriceYak**, **eCommerceFuel** drop-ship tools | Dropshippers are a distinct ICP we may or may not target. |

## What sellers complain about (from public review sites and reddit r/Ebay, r/eBaySellerAdvice)

Repeat themes worth quoting (paraphrased):

1. "eBay Seller Hub charts are useless — I can't see actual profit, just revenue."
2. "Promoted Listings ad spend is a black box; I can't tell which listings actually paid back."
3. "Fee reconciliation is a nightmare — store fees, final value, ads, all on different pages."
4. "Importing eBay into QuickBooks via CSV breaks every time eBay changes export format."
5. "When I run a store across .com and .de, I can't get one P&L."
6. "Repricers are either dumb (race to the bottom) or enterprise-priced."
7. "I want to know which categories actually make me money — not which have the most sales."

Every one of these is a feature we can build that the incumbent above is weak on or missing.

## Where we win

| Vector                                | Our position vs incumbents                                                              |
|---------------------------------------|------------------------------------------------------------------------------------------|
| **True net-margin P&L by listing**    | A2X/LMB stop at GL entries; Sellbrite is shallow. We're the only one combining Finances API + COGS + ad spend + shipping at the listing level. |
| **ML insights**                       | Nobody in this list runs real demand-forecasting/elasticity models per seller. Amazon-side tools (Helium 10) do; eBay-side is a wide open lane. |
| **Combined accounting + inventory + repricer** | The all-in-ones (Sellbrite, 3Dsellers) skimp on accounting. Accounting tools skip everything else. We unify them. |
| **Aesthetics**                        | The category looks like 2015. A 2026-grade UI is a real differentiator.                  |
| **Self-serve pricing under $50**      | Small sellers are priced out of Linnworks; bigger than InkFrog's scope. The $19–$49 lane is uncrowded for "everything in one place." |
| **EU-first compliance** (VAT OSS, EPR)| US-centric tools handle this poorly. EU sellers (UK/DE/FR/IT/ES/PL) are massively underserved. |
| **Public API + webhooks**             | None of the small-seller tools above have a credible API. Power users will love this.    |
| **Native multi-currency**             | Most tools fudge this. Multi-marketplace sellers feel it daily.                          |

## Where we lose (and how to mitigate)

| Vector                            | Risk                                                  | Mitigation                                                  |
|-----------------------------------|-------------------------------------------------------|-------------------------------------------------------------|
| Brand & trust                     | A2X has a decade of accountant trust.                 | Get certified accountant partners; publish reconciliation precision benchmarks. |
| Onboarding complexity             | "Accounting + inventory + ML" sounds heavy.           | First-run wizard hides everything; default to "show me my profit" view. |
| eBay TOS risk                     | eBay can throttle, ban, or compete.                   | Stay strictly within published APIs; never scrape; consider EPN membership. |
| Single-marketplace dependency     | If eBay disconnects us, we die.                       | Add Amazon SP-API by month 9 to de-risk.                    |
| Multi-channel completeness        | Sellbrite, Linnworks list 20+ channels.               | We don't need 20; we need the top 5 done well.              |

## Pricing benchmarks (what the market pays)

| Tier             | Typical monthly      | What's included                              |
|------------------|----------------------|----------------------------------------------|
| Starter (bookkeeping bridge) | $17–$29   | Up to 200 orders/mo, 1 marketplace           |
| Mid (multi-channel listing)  | $49–$99   | 5k orders/mo, 2-3 channels                   |
| Pro (analytics + repricer)   | $99–$249  | 25k orders/mo, full features                 |
| Enterprise                   | $499–$2000+ | Linnworks-class, custom                    |

See [MONETIZATION.md](MONETIZATION.md) for our tier proposal.

## SEO & content competition

The keywords sellers actually search:

- "ebay accounting software" — owned by A2X, Link My Books (their blog content is excellent; we'll need to compete on depth + freshness)
- "ebay fees calculator" — long-tail, easy to outrank with a free tool we build into our marketing site
- "ebay profit calculator" — same
- "ebay repricer" — competitive
- "ebay inventory management" — Sellbrite dominates, beatable
- "ebay to quickbooks" — A2X, LMB
- "ebay finances api" — developer searches, easy lane

Build the free calculators on `ebay-soft.com/tools/*` — they convert and rank.
