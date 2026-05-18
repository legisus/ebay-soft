# eBay API integration

## Programs to register for

1. **eBay Developers Program** account → create an app on https://developer.ebay.com.
2. Three keysets: **Sandbox**, **Production**, and a separate one for **Platform Notifications** if used.
3. **Marketplace Account Deletion / Closure notification endpoint** — eBay requires every production app to expose this. Failing to set it up = your app gets disabled. We must publish a public HTTPS endpoint that:
   - Validates the challenge code (SHA-256 of token + challenge + endpoint URL).
   - Acknowledges the deletion within 24 hours.
   - Hard-deletes that buyer's PII from our DB.

## OAuth 2.0 flow we use

eBay supports **Authorization Code Grant** (for seller apps acting on behalf of a user) — this is what we need.

```
Seller clicks "Connect eBay"
  → 302 to https://auth.ebay.com/oauth2/authorize?client_id=...&redirect_uri=...&response_type=code&scope=...&prompt=login
  → seller logs in & consents
  → eBay redirects to https://ebay-soft.com/oauth/ebay/callback?code=...
  → backend POST /identity/v1/oauth2/token with code → access_token (2h) + refresh_token (18 months)
  → store refresh_token AES-256-GCM encrypted, store access_token in Redis with TTL
```

Refresh: `grant_type=refresh_token`, **reuse the same refresh token** — eBay does NOT rotate it on every refresh. But it does expire after 18 months — the seller will need to re-consent, and we send them an email 14 days before expiry.

### Scopes we request (minimum viable)

```
https://api.ebay.com/oauth/api_scope/sell.account.readonly
https://api.ebay.com/oauth/api_scope/sell.fulfillment.readonly
https://api.ebay.com/oauth/api_scope/sell.fulfillment
https://api.ebay.com/oauth/api_scope/sell.inventory
https://api.ebay.com/oauth/api_scope/sell.inventory.readonly
https://api.ebay.com/oauth/api_scope/sell.finances
https://api.ebay.com/oauth/api_scope/sell.marketing
https://api.ebay.com/oauth/api_scope/sell.marketing.readonly
https://api.ebay.com/oauth/api_scope/sell.analytics.readonly
https://api.ebay.com/oauth/api_scope/commerce.identity.readonly
```

Request the smallest set per feature. Inventory write scope is only requested when the seller turns on the repricer.

## REST APIs we'll consume

| Capability                | API                                | Endpoints we use                                                  |
|---------------------------|------------------------------------|-------------------------------------------------------------------|
| Identity                  | Commerce Identity                  | `GET /commerce/identity/v1/user/`                                 |
| Orders / fulfillment      | Sell Fulfillment                   | `GET /sell/fulfillment/v1/order`, `GET /order/{orderId}`          |
| Listings                  | Sell Inventory + Browse            | `GET /sell/inventory/v1/inventory_item`, `GET /offer`, `PUT /offer` (for repricer) |
| Financials & fees         | **Sell Finances**                  | `GET /sell/finances/v1/transaction`, `GET /payout`                |
| Marketing & promotions    | Sell Marketing                     | `GET /sell/marketing/v1/ad_campaign`, ad reports                  |
| Analytics                 | Sell Analytics                     | `GET /sell/analytics/v1/seller_standards_profile`, traffic report |
| Taxonomy                  | Commerce Taxonomy                  | category tree (cached daily)                                      |
| Browse / search           | Buy Browse                         | competitor lookup for repricer                                    |
| Marketplace insights      | Buy Marketplace Insights           | sold-price data for ML (limited access, requires special approval)|

The **Sell Finances API** is the one that actually gives us correct net-income data — final value fees, store subscription costs, ad costs, refunds, disputes, all on one timeline. This is the difference between "we copy what eBay's hub shows" and "we tell you your real margin."

The legacy **Trading API** (XML/SOAP) is still authoritative for some odd corners (e.g. some store settings). Avoid unless required.

## Rate limits

- Most Sell APIs: 5,000 calls per app per day, **across all sellers**. This is the constraint that matters.
- Hard per-call limits per endpoint vary; eBay returns `429` with a `Retry-After` header — we always honor it.

Implications:

- We **must** batch. Sync uses cursor-based pagination with `limit=200` (max) and processes pages reactively.
- We **must** use **Platform Notifications** (push) instead of polling for high-volume sellers. Sellers can subscribe to events like `ITEM_SOLD`, `FIXED_PRICE_TRANSACTION`, `ITEM_REVISED`. We register a webhook URL and eBay POSTs to us — no polling needed.
- We add per-tenant token-bucket rate limiting in front of the eBay client so one chatty seller can't burn the daily quota for everyone.

## Sandbox vs production

- Sandbox is unreliable — data resets, behavior diverges. Use it only for OAuth flow smoke tests.
- For everything else, use **Wiremock recordings** of real production responses (sanitized) as our integration-test fixtures. Refresh recordings quarterly.

## Compliance items (don't forget)

- eBay's Marketplace Account Deletion notification endpoint (above).
- **Buyer PII**: store the minimum we need (pseudonym + shipping address only for orders we're actively fulfilling). Purge after 90 days.
- **eBay logo and branding** usage must follow their Brand Usage Guidelines. We can say "for eBay sellers" but cannot use the eBay logo in a way that implies endorsement.
- We are NOT an "Authorized Partner" by default — applying to the **eBay Partner Network** is optional and brings perks (higher rate limits, listing in their app directory). Worth doing once we have 50+ paying sellers.

## Error taxonomy we surface to users

| eBay condition                       | Our user-facing message                                  | Action |
|--------------------------------------|----------------------------------------------------------|--------|
| `invalid_grant` on refresh           | "Your eBay connection has expired. Please reconnect."    | Show banner, deep link to OAuth start |
| 429 with `Retry-After`               | "We're catching up — refresh in a moment."               | Auto-retry, no user action |
| 500 / 502 / 503                      | "eBay is having a hiccup. We'll resume automatically."   | Queue and retry with backoff |
| 401 with `User Access Token Revoked` | "You disconnected this app in eBay. Reconnect to resume." | Mark account `expired`, email user |
