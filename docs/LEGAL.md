# Legal — privacy, cookies, terms, why we need user data

What we owe our users in plain language and in formal documents. This file is the engineering reference; the user-facing pages on `ebay-soft.com/legal/*` are written from it.

> **Disclaimer.** This is a working specification, not legal advice. Before billing real customers, have a qualified lawyer in the relevant jurisdictions (at minimum: EU/Poland or Germany if you're EU-incorporated, US/Delaware) review every document below. Budget $1,100–$3,300 for a solid review pass.

---

## Documents we must publish

| Document                            | URL                                       | Why                                                 |
|-------------------------------------|-------------------------------------------|------------------------------------------------------|
| **Terms of Service**                | `/legal/terms`                            | Contract between us and the seller                  |
| **Privacy Policy**                  | `/legal/privacy`                          | GDPR Art. 13/14, CCPA, etc. — required             |
| **Cookie Policy**                   | `/legal/cookies`                          | ePrivacy / GDPR; required if any non-essential cookies |
| **Data Processing Agreement (DPA)** | `/legal/dpa` (also downloadable PDF)      | We're a processor of buyer PII; sellers need this for their own GDPR compliance |
| **Sub-processor list**              | `/legal/subprocessors` (public, dated)    | Required for transparent processor relationships    |
| **Acceptable Use Policy**           | `/legal/aup`                              | What you can't do on our platform                   |
| **Refund Policy**                   | `/legal/refunds`                          | EU distance-selling law + general clarity           |
| **Security & Disclosure**           | `/legal/security`, `/.well-known/security.txt` | Vulnerability reporting channel               |
| **Imprint / Legal Notice**          | `/legal/imprint`                          | Required by DE/AT/CH law if targeting those markets (Impressumspflicht); good practice everywhere |

---

## Why we need user data — the explanation users actually deserve

This is the heart of the Privacy Policy. Treat it as user-facing copy, not lawyer copy. Every piece of data must answer: *what*, *why*, *legal basis*, *how long*, *who else sees it*.

### Categories of data we collect

| Data                                       | Why we need it                                                  | GDPR lawful basis (Art. 6)        | Retention                                  |
|--------------------------------------------|------------------------------------------------------------------|------------------------------------|--------------------------------------------|
| Email, password hash                       | Account identity & login                                         | (b) Contract                       | Until account deletion + 30 days           |
| 2FA / TOTP secret                          | Account security                                                 | (b) Contract                       | Until disabled or account deletion         |
| Phone number (E.164)                       | SMS-based 2FA (opt-in), account-recovery verification            | (a) Consent + (f) Legitimate interest for security | Until removed by user or account deletion + 30 days |
| eBay user ID, marketplace                  | Linking your eBay account to ebay-soft                           | (b) Contract                       | Until you disconnect or close account      |
| eBay OAuth refresh token (encrypted)       | Calling eBay's API on your behalf                                | (b) Contract                       | Until you disconnect; rotated automatically |
| Orders, listings, fees, payouts (from eBay)| Computing P&L, accounting reports, inventory, repricing          | (b) Contract                       | Lifetime of account + 7 years (tax laws)   |
| Buyer pseudonym, address (from eBay)       | Showing you who an order shipped to; never used by us otherwise  | (b) Contract (you fulfil the order; we're a processor) | 90 days after order completion |
| Stripe customer ID, last4 of card, plan    | Billing & subscription management                                | (b) Contract                       | Lifetime of account + 7 years (tax)        |
| IP address, user-agent, login history      | Security: detect brute force, impossible-travel, fraud           | (f) Legitimate interest            | 12 months                                  |
| Audit log (every privileged action)        | Security & compliance; investigating incidents                   | (c) Legal obligation + (f) Legit. interest | 24 months                          |
| Product usage (page views, feature use)    | Improving the product                                            | (f) Legitimate interest — analytics with cookieless tooling (Plausible) | 14 months |
| Support conversations                      | Resolving your issue & training our support                      | (b) Contract + (f) Legit. interest | 24 months                                  |
| Marketing-email subscription state         | Sending you newsletters/announcements                            | (a) Consent                        | Until you unsubscribe                      |

### What we do NOT collect

State this loudly on the privacy page:

- We don't sell data.
- We don't share with advertisers.
- We don't fingerprint you across other sites.
- We don't aggregate your buyer data into a data product without explicit consent.
- We don't train AI models on your private data without explicit opt-in (this matters more every year).

### Special handling: buyer PII vs seller PII

Sellers are **our customers** (we are a controller for their account data, processor for the buyer data their account brings in).
Buyers are **the seller's customers** (we are a processor on the seller's behalf for their PII; the seller is the controller).

So a seller's GDPR rights are exercised against us directly. A buyer's GDPR rights are exercised against the seller, with us assisting per the DPA.

We minimize buyer data: pseudonym + city/country in dashboards; full address only on the order-detail screen when the seller actively looks at it, and even then we redact street/number unless the seller clicks "show full address." A 90-day TTL purges what we don't need.

---

## Sub-processors (public list)

Maintained at `/legal/subprocessors` with a "last updated" date. Sellers must be notified 30 days before a new sub-processor is added.

| Sub-processor          | What we use them for                      | Location of data processing      | Transfer mechanism          |
|------------------------|-------------------------------------------|----------------------------------|------------------------------|
| **Hetzner Online GmbH**| Server hosting                            | Falkenstein, Germany             | None needed (EU→EU)         |
| **Cloudflare**         | CDN, WAF, DNS, TLS                        | EU edge + US HQ                  | SCCs + DPA                  |
| **Stripe Payments**    | Subscription billing, tax calculation     | EU (Ireland) + US                | SCCs + DPA                  |
| **Postmark** (or Resend) | Transactional email                     | US                               | SCCs + DPA                  |
| **Twilio**             | SMS-based 2FA OTP delivery (Twilio Verify API) | EU (Dublin) for EU customers; US for the rest | SCCs + DPA |
| **Sentry**             | Error monitoring                          | EU region                        | None needed (EU region)      |
| **Anthropic / OpenAI** | AI title-rewrite (only when seller uses the feature) | US               | SCCs + DPA; opt-in per tenant |
| **Plausible Analytics**| Cookieless product analytics              | EU (Germany)                     | None needed (EU)             |

We do NOT add a sub-processor before this list is updated and 30 days have elapsed for paying customers.

---

## Cookies — EU vs US

### What we'll actually set at MVP

- **Strictly necessary:** session cookie (httpOnly, secure, sameSite=strict), CSRF cookie, language preference. These do **not** require consent under GDPR/ePrivacy.
- **No analytics cookies.** We use Plausible (server-side, cookieless). This is the deliberate decision that lets us avoid the cookie banner entirely.
- **No marketing cookies, no third-party ad pixels.** If we add Google Ads or Facebook Pixel later, that's when a banner appears.

Practical effect: **at MVP we don't need a cookie consent banner.** We do publish `/legal/cookies` listing the strictly-necessary cookies for transparency.

### When a banner becomes required

- **EU/UK (ePrivacy + GDPR):** any non-essential cookie or local-storage write requires **opt-in** before it fires. "Reject all" must be as prominent and as easy as "Accept all." Pre-ticked boxes are not valid consent (Planet49 case, CJEU 2019).
- **California (CCPA/CPRA), Virginia, Colorado, Connecticut, Utah, Texas, etc.:** **opt-out** model. Need a "Do Not Sell or Share My Personal Information" link if applicable; "Sell or Share" is broadly defined and may include analytics in some interpretations.
- **Quebec Law 25, Brazil LGPD:** opt-in, similar to GDPR.

### How to implement when we need it

Use a **CMP (Consent Management Platform)** — don't roll your own:

| Option                       | Cost                                       | Notes                                                          |
|------------------------------|--------------------------------------------|-----------------------------------------------------------------|
| **Cookiebot** (Usercentrics) | $12–$65/mo (billed in EUR by the vendor)    | Mature, well-trusted, IAB TCF v2 compliant                     |
| **Iubenda**                  | $30+/mo (billed in EUR by the vendor)       | Includes Privacy & Cookie Policy generator; bundled                  |
| **Termly**                   | $10–$33/mo                                  | Cheaper, US-focused, decent EU compliance                       |
| **Osano**                    | Free tier exists                            | Solid, US-headquartered                                         |
| **Klaro!** (open-source)     | Free, self-hostable                         | Works; less polished UX; we own it                              |
| **Plain HTML banner**        | Free                                        | Only acceptable if you have one cookie and a competent dev; risk: regulators don't take "we coded it ourselves" as a defense in audits |

Geolocation: branch banner behavior on Cloudflare's `CF-IPCountry` header. EU country → opt-in. US California / VA / CO / CT / UT / TX → opt-out link + standard preferences. Everywhere else → opt-out (more conservative than required).

---

## GDPR / CCPA mechanics we must support

Most of these are already in [SECURITY.md](SECURITY.md); restating for legal clarity:

### Self-serve rights

- **Right of access** — `GET /v1/me/export` returns a zip of everything we have on you, machine-readable.
- **Right to rectification** — settings UI; otherwise email support.
- **Right to erasure ("right to be forgotten")** — self-serve "Delete my account" button → 30-day soft-delete window with email → hard delete (anonymized analytics & audit-log entries retained where law requires).
- **Right to data portability** — same as access.
- **Right to object / opt-out** — toggles for marketing email; toggle for "do not use my data to improve features" (we'll respect it by suppressing inclusion in any aggregate analytics modeling).
- **Right to restrict processing** — supported via support email; rare in our use case.

### Mandatory disclosures

- **Identity & contact of the controller** — company legal name, address, email, DPO if appointed.
- **Purpose & legal basis** for each processing activity (see table above).
- **Recipients** — the sub-processor list, updated.
- **International transfers** — we use SCCs where data leaves the EU; list which sub-processors involve transfers.
- **Retention periods** — explicit per category.
- **Rights** — listed, with how to exercise.
- **Right to complain to a supervisory authority** — name the seller's local DPA.
- **Whether providing data is statutory / contractual** — and what happens if you don't provide it.
- **Existence of automated decision-making** — we don't currently do solely-automated decisions with legal effect; the repricer pushes price changes only under rules the seller configures, so a human is always upstream. Disclose this anyway.

### Incident notification

- **72-hour breach notification clock** to the supervisory authority starts at the moment we have *confirmed* an unauthorized disclosure of PII. We notify affected sellers without undue delay too.
- A pre-drafted notification template lives in `/ops/runbooks/breach-notification.md`.

---

## ToS — the must-haves

A defensible ToS at MVP must include:

1. **Definitions** — Service, User, Subscription, Content.
2. **Account creation & eligibility** — must be 18+, must have legal authority to act for the eBay account being connected.
3. **License grant from us to you** — limited, revocable, non-transferable.
4. **License grant from you to us** — explicit grant to process your data to provide the service; clear scope limits.
5. **Acceptable use** — link to AUP.
6. **Fees, billing, taxes, refunds** — link to pricing page and refund policy.
7. **Trial terms** — 14-day Pro trial, no card required, auto-downgrades to Free.
8. **Termination** — by you anytime; by us for cause with notice.
9. **Disclaimers** — service "as is," no guarantee of revenue increase or eBay TOS compliance for your behavior.
10. **Limitation of liability** — capped at fees paid in the prior 12 months. Standard SaaS clause.
11. **Indemnification** — you indemnify us for misuse, especially for ToS violations of eBay itself.
12. **Governing law & jurisdiction** — the country of your legal entity; consumer protections preserved where mandatory.
13. **Changes to terms** — 30-day notice for material changes.
14. **eBay-specific disclaimers** — "ebay-soft is not affiliated with eBay Inc.; eBay is a trademark of eBay Inc."

---

## DPA — what enterprise customers will ask for

A clean Data Processing Agreement (template available on the legal page, signable via PDF or Vanta-style flow):

- We act as **processor** for buyer PII; seller is the controller.
- Categories of data, categories of data subjects, processing purposes — explicit.
- Sub-processor list reference + notification mechanism.
- Security measures (encryption at rest & transit, access controls — references [SECURITY.md](SECURITY.md)).
- Sub-processor flow-down obligations.
- Audit rights (annual; SOC 2 report once we have one).
- Return / deletion of data on termination.
- Standard Contractual Clauses for non-EU transfers, attached as exhibits.

Use the [IAPP DPA template](https://iapp.org/) or Vanta's generator as a starting point; lawyer-review before signing the first enterprise deal.

---

## eBay-specific legal obligations

These are not optional:

1. **API License Agreement** — we accept it when we create the dev account. Read it. Key rules: no scraping outside the API, no caching beyond TTLs they specify, no re-distribution of marketplace data, no using their data to build a competing marketplace, attribution where required.
2. **Marketplace Account Deletion / Closure Notification** — public HTTPS endpoint, validated, monitored, action within 24 hours. Failing this disables our app.
3. **Brand / trademark usage** — we can say "for eBay sellers" (descriptive). We cannot use eBay's logo or imply endorsement. We must not register a domain that confuses with eBay's own (we're "ebay-soft.com" not "ebaysoftware.com" — the hyphen and the descriptor help).
4. **eBay Partner Network** (optional) — if we join, additional rules around attribution and revenue sharing apply.

---

## US-specific gotchas

- **California Privacy Rights Act (CPRA)** — applies once we have CA residents using us. The "Do Not Sell or Share" link is conservative; consult counsel on whether our use of analytics counts as "sharing." Plausible-only likely doesn't, but get a real opinion.
- **State privacy laws cascade** — VA, CO, CT, UT, TX, IN, IA, FL, OR, MT have or are getting their own. A single privacy notice tailored to "CCPA + sibling laws" generally covers all.
- **1099-K reporting** — if we ever pay creators or affiliates >$600/year (US), we issue 1099-NEC/1099-K. Stripe Connect handles this if we add an affiliate program.
- **CAN-SPAM** — every marketing email must have unsubscribe + physical mailing address. Postmark/Resend templates do this; verify.

---

## EU-specific gotchas

- **Impressum (DE/AT/CH)** — required legal-notice page if targeting German-speaking markets. Trivial to write; expensive to omit (Abmahnungen are a cottage industry).
- **Distance-selling rights** — EU consumers have a 14-day cancellation right. For SaaS provided digitally that you've consented to start before the 14 days, the right can be waived — wording matters. Use Stripe's "Service starts immediately, I waive my right to withdraw" checkbox in Checkout.
- **VAT MOSS / OSS** — if we sell to EU consumers, we charge VAT at their rate. Stripe Tax handles this once enabled and configured. Required for any EU sale.
- **B2B vs B2C** — collecting VAT IDs from business customers in Checkout enables reverse-charge; saves both sides money. Stripe Tax does this.
- **Polish-specific (if entity is sp. z o.o.)** — JPK_VAT reporting; an accountant handles it.

---

## Operational checklist before charging the first customer

- [ ] Privacy Policy published, reviewed by counsel
- [ ] Terms of Service published, reviewed by counsel
- [ ] Cookie Policy published (even if it says "we only use essential cookies")
- [ ] DPA template available
- [ ] Sub-processor list published with current date
- [ ] AUP published
- [ ] Refund Policy published
- [ ] Imprint published (if any DACH-market traffic)
- [ ] `security.txt` published with PGP key
- [ ] GDPR self-service export endpoint working
- [ ] GDPR self-service deletion endpoint working
- [ ] eBay Marketplace Account Deletion endpoint verified
- [ ] Stripe Tax enabled, jurisdictions registered (at minimum: our EU country + nexus states)
- [ ] Trademark filings submitted (or scheduled)
- [ ] Legal entity registered, bank account open, accountant retained
- [ ] D&O insurance + cyber-liability insurance quotes obtained (decide whether to buy at launch or at $10k MRR)
- [ ] Backup & restore drill completed (proves we can honor "right to access" and "right to be forgotten")

This list is what makes the difference between "I built a SaaS" and "I built a SaaS I can charge real money for."
