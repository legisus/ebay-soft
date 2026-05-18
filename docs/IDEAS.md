# Ideas — what makes ebay-soft genuinely interesting

This is the dedicated "differentiation" file. Everything in [FEATURES.md](FEATURES.md) is table stakes plus enhancements. This file is about the ideas that, if executed well, give a seller a reason to **pick us** instead of the cheaper accounting bridge they're already using.

Each idea has: **What**, **Why it's interesting**, **How hard**, **Why incumbents don't have it**.

---

## 1. The Profit Black Box exit interview

**What:** For every listing, show a "where did the money actually go?" view: sale price → final value fee → ad cost → promoted-listings cut → shipping you paid vs. shipping you charged → returns reserve → COGS → net. Animated Sankey or waterfall, drillable.

**Why it's interesting:** Sellers know their revenue. They don't know their margin per item. This is the single most repeated complaint about eBay's native hub. Show it on first login and it's the screenshot they share on Twitter.

**How hard:** Medium. Needs the Finances API joined to orders and seller-entered COGS. The math is straightforward; the UX is the work.

**Why incumbents don't:** A2X/LMB stop at general-ledger entries. Sellbrite shows revenue. Nobody combines all sources at the listing level.

---

## 2. "Why this listing isn't selling" diagnostic

**What:** Click any underperforming listing → a structured report:

- Title length (51/80 chars used) — top sellers in category use 76.
- Aspect coverage — you have 6 of 14 recommended aspects filled.
- Image count — you have 3, top performers use 9.
- Image quality — image 2 is 480×360, eBay recommends ≥1600.
- Price percentile — your price is at the 78th percentile in your category; sell-through is highest at the 45th–55th.
- Shipping ratio — you charge $8 shipping on a $30 item (27%); category median is 12%.
- Listing age — 142 days; algorithm boosts new listings.
- Visibility — Promoted Listings off; competitors at this price are all promoting.

Each row is actionable, with a one-click "fix" where we can do it.

**Why interesting:** Sellers staring at a dead listing have no idea what's wrong. This is the "lighthouse audit" for eBay listings.

**How hard:** Medium. Most signals come from the Sell Inventory API + category taxonomy + a small image-quality model. Aspect-completeness needs the taxonomy aspects per category, which eBay provides.

**Why incumbents don't:** It needs all the data sources in one place — accounting tools don't have listings; listing tools don't have category benchmarks; analytics tools don't have image checks. We're the only one combining them.

---

## 3. The Margin Floor repricer (anti-race-to-the-bottom)

**What:** A repricer whose primary input is **your margin**, not your competitors'. Every rule has a margin floor that's recomputed live as fees and ad spend change. The repricer will refuse to drop below that floor, full stop. If the competition is selling below your floor, we tell you and stop, not race them.

**Why interesting:** Existing repricers (Repricer.com, RepricerExpress) are still optimized for "win the buy box / be cheapest." On eBay there's no buy box for most items, so the race-to-zero math is even worse. A margin-first repricer is novel.

**How hard:** Medium. The margin computation is the hard part; the actual price-push is one PUT call.

**Why incumbents don't:** Amazon-trained engineers built the existing repricers and brought Amazon's buy-box mental model. eBay's economics are different.

---

## 4. "Sourcing radar" — what should you sell next?

**What:** Based on the seller's category history, their margin profile, and their reorder cadence, we propose 5–10 SKU archetypes worth testing. Example output:

- "You sell vintage German cameras at $120 average, 34% margin, ~18 days to sell. Look at: vintage German lens accessories — similar buyers, lower acquisition cost, faster turnover (median 7 days based on completed listings)."

**Why interesting:** It's a recommendation engine that respects margin, not just GMV. It also keeps sellers expanding their inventory inside our app instead of using a separate sourcing tool.

**How hard:** Hard. Needs marketplace insights (paid/approved) or careful use of Browse API + completed-listings data. ML model is a content-based recommender on category embeddings, then filtered by feasibility.

**Why incumbents don't:** Sourcing tools exist (Terapeak, Zik Analytics) but they're cross-seller benchmarks, not personalized recommendations grounded in the seller's actual unit economics.

---

## 5. Auto-VAT for EU sellers (OSS / IOSS)

**What:** Identify every line that crosses an EU border, apply the correct VAT rate by destination country and product category, prepare a one-click OSS quarterly return. Same for IOSS for imports ≤€150 (a real EU regulatory threshold, kept in EUR). Plug into Avalara or just compute directly using the EU VAT API.

**Why interesting:** EU VAT compliance after OSS rules became messy. EU sellers pay accountants $220–$1,100/quarter just to sort this out. Solving it well is worth $30/mo by itself.

**How hard:** Medium-hard. The taxonomy mapping (category → product type → VAT rate per country) is the slog. Once built, it's evergreen.

**Why incumbents don't:** US-centric tools don't touch EU VAT. EU accounting bridges expose CSVs but don't compute returns.

---

## 6. The Negative-Feedback Insurance bot

**What:** Detect risky orders in real time (slow-paying buyer, mismatched shipping address, low buyer feedback, dispute history) and automatically:

- Send a templated friendly message.
- Hold shipment for review.
- Apply tracked+signed-for shipping.
- Open a dispute proactively before the buyer does.

Each rule is opt-in; we show the predicted defect-rate impact.

**Why interesting:** Defect rate moves a seller's status, which moves their visibility, which moves their revenue. A tool that defends the defect-rate score is high-leverage.

**How hard:** Medium. Detection is mostly rules; orchestration is a workflow engine.

**Why incumbents don't:** 3Dsellers has auto-message templates, but no risk model. The risk model is the differentiator.

---

## 7. Promoted-Listings counterfactual ROI

**What:** For every promoted-listings campaign, show **counterfactual** net profit — not just gross "ad spend vs. revenue from promoted clicks," but *"X% of these sales would have happened organically anyway based on your baseline; the true incremental contribution of the ad spend is Y."* Computed via difference-in-differences against unpromoted baseline periods, unpromoted control SKUs, or comparable category baselines.

**Why interesting:** This is now urgent, not nice-to-have. In January 2026 eBay changed how it attributes sales to promoted campaigns, and seller forums lit up with people who discovered their "profitable" ad strategy was a fee on sales that would have happened anyway. Gross-ROI dashboards (which every incumbent shows, when they show anything at all) actively *hide* this. A counterfactual model is the antidote.

**How hard:** Medium-hard. Diff-in-diff against unpromoted SKU controls is a standard causal-inference pattern; we just need enough historical data to compute baselines per category. Initial version can ship with simple before/after pause windows; richer model uses synthetic-control or geo-experiments later.

**Why incumbents don't:** They show what the eBay Marketing API gives them, which is gross attribution. Computing the counterfactual requires a model, and no current eBay-side tool runs models.

---

## 8. "Tax Day Mode" — one-button accountant export

**What:** A single button at the end of a tax period that produces, packaged in a zip:

- P&L (PDF + XLSX) in the seller's local accounting format.
- Categorized transaction journal (CSV).
- Inventory valuation at period end (FIFO + LIFO + WAC, the accountant picks).
- 1099-K / VAT / GST reconciliation worksheet for the relevant jurisdiction.
- Source documents (eBay invoices, payouts).

Send it straight to the accountant's email with a polite cover note.

**Why interesting:** Accountants hate eBay sellers because the data is messy. Be the seller who sends a perfect, sealed package and you become the accountant's favorite client. Accountants then refer their other eBay sellers to us — distribution channel unlocked.

**How hard:** Mostly content/packaging; we already have the data.

**Why incumbents don't:** They sell to sellers, not accountants. Accountants are a hidden growth lever.

---

## 9. Inventory financing scorecard

**What:** Compute a "financing-readiness" score: sell-through rate, inventory-turnover, gross margin, return rate, payout cadence. Offer a button: "share this with a lender." Partner with one or two e-commerce lenders (Wayflyer, Settle, Clearco-style) and earn a referral fee.

**Why interesting:** Working capital is the #1 growth blocker for small sellers. We're sitting on the exact data lenders want. This is real revenue from a side channel.

**How hard:** Easy to score; hard to partner. Partnership conversations should start once we have ~100 paying sellers (data critical mass).

**Why incumbents don't:** They aren't thinking about adjacent revenue. We can be first.

---

## 10. Replay & rewind — "what would my account look like if I'd done X?"

**What:** Time-machine view. Pick a date in the past, simulate: "what if I'd run the repricer with strategy Y from that day?" or "what if I'd promoted these 50 listings at 5% ad rate?" Show the alternate P&L.

**Why interesting:** Sellers learn from this. They also share screenshots. Marketing gold.

**How hard:** Medium. We already have the historical data; we need a small simulator.

**Why incumbents don't:** Nobody bothers.

---

## 11. "Profit Mode" toggle — show price-tags as net profit

**What:** Across the entire dashboard, an always-visible toggle: show prices, or show **net profit**. Every chart re-renders. Every table column flips. Seeing net margin in green/red next to every SKU rewires how sellers think about their business.

**Why interesting:** Small UX detail, huge mindset shift. This becomes our brand promise.

**How hard:** Easy if the data model is right from day one. Hard if retrofitted later — design it now.

**Why incumbents don't:** They show what eBay shows.

---

## 12. Built-in image background remover & specs

**What:** Drop an image in, get out a clean white-background, eBay-spec-compliant (≥1600 px on the long side, padding correct) JPG ready to upload. Free for paying tiers. Uses a self-hosted ONNX `rembg` model.

**Why interesting:** Sellers pay $5–15 per image to outsource this. Free with our subscription is a measurable savings narrative.

**How hard:** Easy. Open-source model, small infra cost.

**Why incumbents don't:** Most don't do images at all.

---

## 13. "Brand voice" listing rewriter

**What:** Seller defines their voice (tone, banned words, must-include phrases, signature line). Click any listing → AI rewrite proposes new title and description in that voice. Seller approves before publishing.

**Why interesting:** AI listing tools exist but they all sound the same. Voice-aware rewriting respects what makes a brand a brand. Sellers stop sounding like every other dropshipper.

**How hard:** Easy (LLM call), but the prompt scaffolding and approval UX is real work.

**Why incumbents don't:** AI features in this space are mostly afterthoughts.

---

## 14. Real-time "competitor activity" feed

**What:** A live feed: "competitor X relisted same SKU at $12.40 (was $13.90) 2 minutes ago." "Three new sellers entered category Y this week." Optional Telegram pings.

**Why interesting:** Sellers obsess about competitors. Making this feed clean and timely is sticky.

**How hard:** Medium. Browse API polling per watched-listing, smart polling cadence.

**Why incumbents don't:** Repricers do half of this but bury it.

---

## 15. Free public tools as marketing (SEO)

**What:** Build, on the marketing site, free tools that rank and convert:

- **eBay Fee Calculator** (final value + store + ads + payment processing per category per country) — one input, one big number, "want to track this automatically? sign up."
- **eBay Profit Calculator** — like above + COGS + shipping.
- **eBay Title Scorer** — paste a title, get a score and improvement tips.
- **VAT Calculator for eBay sellers** — EU-focused.
- **Image background remover** — free, watermark-free.

**Why interesting:** Each tool is a top-3 Google result waiting to happen, drives free signups, and is cheap to build.

**How hard:** Each is a weekend. Together they are 60% of our top-of-funnel.

**Why incumbents don't:** Some have one or two. None have all five well-executed.

---

## 16. Slack/Telegram bot — daily morning brief

**What:** Every morning, your seller's preferred chat tool pings with: "Yesterday: 47 orders, $1,243 revenue, $312 net (24.5% margin). Top SKU: X. Low stock: Y, Z. New dispute on order #..."

**Why interesting:** Daily touch point. Sellers love the feeling of running a business that reports to them. Retention metric goes up.

**How hard:** Easy.

**Why incumbents don't:** They live inside a web app. We live where the seller already lives.

---

## 17. White-label / agency mode

**What:** Phase 3. Agencies and bookkeepers managing many eBay sellers get a master tenant view, white-labeled reports with their own logo, single-bill across all client accounts.

**Why interesting:** Per-seat economics for agencies are great. One agency = 10–50 paying sellers.

**How hard:** Medium-hard. Touches every screen.

**Why incumbents don't:** A2X has a partner program but no real agency view.

---

## 18. "We caught eBay's mistake" auditor

**What:** Automated audit that flags discrepancies in eBay's own reports — duplicate fees, refund-not-credited, dispute resolved-in-your-favor-but-money-not-returned, payout off by more than $0.01 from order sum.

**Why interesting:** eBay genuinely makes mistakes. Returning $200/month to a seller in caught errors **pays for the subscription forever**. This is the killer retention story.

**How hard:** Medium. Reconciliation logic with tolerance bands.

**Why incumbents don't:** A2X reconciles but doesn't actively flag discrepancies in the seller's favor.

---

## 19. Scam-defense suite — Empty Box / Address-Change / Buyer-Risk

**What:** Three coordinated defenses against the most-reported scams in eBay seller forums:

1. **Address-change interceptor.** When a buyer messages asking to ship to a different address than the order's, we detect it (NLP on the message thread + automatic flag on any change to the shipping field), put the order on hold, and warn the seller in plain language that shipping there voids eBay seller protection (because eBay only covers the order address). One click to refuse, one click to override with logged acknowledgment.
2. **Pack-out evidence capture.** A phone or webcam workflow that records a timestamped video of the item being packed, with the order number and SKU overlaid. Stored to MinIO as immutable, signed-URL evidence. Auto-attached to any subsequent dispute. Defeats the "empty box" and "broken replica" scams that forum sellers lose constantly because they can't prove what they shipped.
3. **Buyer-risk score per order.** 🟢/🟡/🔴 badge based on feedback age, dispute history, return-rate, account-age, velocity, and (Phase 3) cross-tenant defective-claim history (see #22). Visible on every order; high-risk orders get tracked+signature shipping by default.

**Why interesting:** Defect rate moves a seller's eBay status, which moves their visibility, which moves their revenue — the highest-leverage defensive feature we can ship. Forums are saturated with these three named scams; we'd be the only tool that systematically defends against all three.

**How hard:** Medium. Address-change detection is rules + a light classifier on message threads. Pack-out capture is a small PWA + MinIO upload. Risk score is feature engineering over data we already have.

**Why incumbents don't:** Pack-out evidence requires a workflow at the *seller's* physical packout moment — no existing tool spans the digital → physical → digital arc. Address-change detection requires reading buyer messages, which most tools don't ingest.

---

## 20. Bank-chargeback defense workflow

**What:** Chargebacks that bypass eBay's Money Back Guarantee — the buyer's bank forces a reversal weeks or months later, regardless of how eBay's own dispute resolved. eBay does little to help; the seller faces the bank's representment process cold. We:

- Detect chargeback events from Stripe / eBay payments webhooks in real time
- Auto-assemble the full evidence packet: tracking + signature, listing photos at time of sale, original buyer-seller messages, pack-out video (#19), the buyer's feedback history, the eBay dispute outcome if any
- Pre-fill the bank's representment template per network (Visa/Mastercard/Amex/Discover) with the seller's narrative skeleton
- Track each case to resolution and learn from outcomes

**Why interesting:** A single saved chargeback on a $500 item pays for a year of subscription. Forum sellers describe losing $1,000+ chargebacks they would have won with organized evidence. High-leverage for Pro/Scale tier — a few wins per quarter is real money.

**How hard:** Medium. Webhook plumbing is straightforward; the value is in the per-network template logic and evidence-packet assembly.

**Why incumbents don't:** Chargeback tools exist as standalone products (Chargeflow, Chargebacks911) at high prices; they don't have the eBay-context data we do. We're the only player who can fold this into a $99/mo plan because we already have all the inputs.

---

## 21. Replacement-instead-of-refund flow

**What:** When something goes wrong with an order, eBay's native flow forces the buyer into a refund/return. Sellers repeatedly ask in forums for an option to **send a replacement** without the refund detour. We orchestrate it: detect a viable case, propose "send replacement" to the buyer (one-tap accept in their email), cancel the dispute if open, mint a linked replacement order in our system, ship it, track it, and close the loop with a customer-facing message thread.

**Why interesting:** Saves the original sale (instead of refunding fees + losing the item), saves a defect-rate hit, and gives the buyer a faster outcome — everyone wins. The pain is real and named in multiple forum threads.

**How hard:** Medium. We don't fight eBay's flow; we offer an alternative the seller and buyer can both agree to, then keep both sides updated. State-machine work + eBay messaging API + careful UX.

**Why incumbents don't:** Requires touching the order workflow + buyer messaging + accounting — three things no incumbent owns simultaneously.

---

## 22. Cross-tenant defective-claim network defense

**What:** A privacy-respecting, opt-in network that tracks buyers (by a salted hash of their eBay buyer-ID) who serial-file "item not as described" / "defective" / "INAD" claims across many sellers. When a flagged buyer's order lands in any opted-in seller's queue, the buyer-risk score (#19) lights up red with the network signal: "this buyer has filed 4 INAD claims across 17 sellers in the last 12 months."

**Why interesting:** Network-effect feature — gets stronger as more sellers join, hard to displace once entrenched. Forum threads are full of named-and-shamed serial-claimers; making that institutional knowledge automatic is genuinely new.

**How hard:** Medium-hard. The hard part is privacy: only hashed IDs, opt-in both ways, GDPR-defensible, no PII leakage between tenants. The score is computed centrally in `analytics-api`; sellers see only an aggregate signal, never the underlying claim list.

**Why incumbents don't:** No incumbent is positioned for a multi-tenant network effect; most are single-tenant tools. Pulling this off requires both the data (orders + claims across tenants) and a privacy/legal posture we'd build in from the start ([LEGAL.md](LEGAL.md)).

---

## The big picture

If we had to pick three ideas to lead with — the things that get screenshots, drive word of mouth, and justify the price tag:

1. **Profit Black Box** (#1) + **Profit Mode toggle** (#11) — our visual identity.
2. **"Why this listing isn't selling" diagnostic** (#2) — our most-shared screenshot.
3. **"We caught eBay's mistake" auditor** (#18) — our retention story.

After the 2026-05 forum research, three more to invest in early:

4. **Scam-defense suite** (#19) — the defensive feature every forum thread asks for.
5. **Bank-chargeback defense workflow** (#20) — a few wins per year pays for years of subscription.
6. **Counterfactual Promoted-Listings ROI** (#7, refined) — solves a pain that's *trending right now* because of eBay's January 2026 attribution change.

Everything else stacks on top.
