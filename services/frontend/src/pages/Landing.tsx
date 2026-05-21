import { Link } from "react-router-dom";

/** Issue date in the masthead. Static for now — could roll daily on a real publication. */
const ISSUE_DATE = new Date().toLocaleDateString("en-GB", {
  weekday: "long",
  day: "numeric",
  month: "long",
  year: "numeric",
});

/** Stats shown in the marquee. Duplicated in render so the ticker scrolls seamlessly. */
const TICKER_ITEMS = [
  { label: "Avg margin uplift", value: "+12.4%", tone: "forest" },
  { label: "Time to first sync", value: "30s" },
  { label: "Marketplaces", value: "12" },
  { label: "Fee categories tracked", value: "47" },
  { label: "Bottom-line accuracy vs eBay reports", value: "99.6%" },
  { label: "Stockouts prevented (private beta)", value: "2,418" },
  { label: "Avg trial-to-paid conversion", value: "31%" },
];

const DEPARTMENTS = [
  {
    kicker: "I. Accounting",
    title: "Reconcile every penny.",
    lede:
      "Live P&L stitches your orders, refunds, ad spend, and the 40+ eBay fee categories into one statement. Export to CSV, XLSX, or signed PDF.",
    detail:
      "Catches the discrepancies eBay's monthly statements quietly fold in. Average user finds $214 in mis-billed fees in their first month.",
  },
  {
    kicker: "II. Sync",
    title: "Your inventory, in one place.",
    lede:
      "Orders, listings, finance events, and disputes — pulled continuously through the Sell APIs and normalized into one schema you can query.",
    detail:
      "Sub-30-second propagation. Resumes from a watermark on every restart so a 3am hiccup doesn't cost you a day of data.",
  },
  {
    kicker: "III. Intelligence",
    title: "Decisions, not dashboards.",
    lede:
      "Repricer, sourcing radar, listing optimizer. The AI doesn't just show you trends — it proposes the move and lets you ship it in one click.",
    detail:
      "Honest pricing: only enabled on Growth plan and up, where the lift more than covers the seat.",
  },
];

export default function Landing() {
  return (
    <div className="grain min-h-screen text-[var(--color-ink)]">
      {/* ─── Masthead ─────────────────────────────────────────────── */}
      <header className="rule-h-double">
        <div className="mx-auto flex max-w-7xl items-baseline justify-between px-6 py-4 md:px-10">
          <Link to="/" className="flex items-baseline gap-3">
            <span className="font-display text-2xl font-medium tracking-tight">
              The eBay-Soft Ledger
            </span>
            <span className="hidden font-mono text-[10px] uppercase tracking-[0.2em] text-[var(--color-ink-faded)] sm:inline">
              · For sellers who know their numbers
            </span>
          </Link>
          <div className="flex items-baseline gap-6">
            <span className="hidden font-mono text-[10px] uppercase tracking-[0.18em] text-[var(--color-ink-faded)] md:inline">
              Vol. I · Issue 001 · {ISSUE_DATE}
            </span>
            <Link to="/login" className="text-sm link-underline">
              Sign in
            </Link>
          </div>
        </div>
      </header>

      {/* ─── Hero ─────────────────────────────────────────────────── */}
      <section className="mx-auto max-w-7xl px-6 pt-16 pb-12 md:px-10 md:pt-24 md:pb-20">
        <p className="kicker reveal reveal-1">An accounting publication, daily</p>
        <h1 className="headline-xl mt-5 max-w-[12ch] text-[clamp(3.25rem,8vw,7.5rem)] reveal reveal-2 md:max-w-[16ch]">
          Run your eBay store like a{" "}
          <em className="not-italic" style={{ color: "var(--color-oxblood)", fontStyle: "italic" }}>
            trading desk.
          </em>
        </h1>

        <div className="mt-12 grid grid-cols-1 gap-10 md:grid-cols-12">
          <div className="md:col-span-7">
            <p className="dropcap font-display text-[1.15rem] leading-[1.55] reveal reveal-3 md:text-[1.25rem]">
              For the operator who treats their store like a business, not a hobby. eBay-Soft
              consolidates orders, fees, refunds, and ad spend into one continuous statement —
              so the number on your dashboard is the number you can defend to your accountant,
              your spouse, and tax season.
            </p>
            <div className="mt-9 flex flex-wrap items-center gap-4 reveal reveal-4">
              <Link to="/signup" className="btn-ink">
                Open an account →
              </Link>
              <Link to="/login" className="btn-ghost">
                Sign in
              </Link>
              <span className="ml-2 font-mono text-[12px] text-[var(--color-ink-faded)]">
                14-day trial · no card · cancel anytime
              </span>
            </div>
          </div>

          {/* Editorial sidebar — "Today's reading" */}
          <aside className="rule-h-double pt-6 md:col-span-4 md:col-start-9 md:border-t-0 md:border-l md:pl-8 md:pt-0">
            <p className="kicker">In this issue</p>
            <ol className="mt-4 space-y-3 font-display text-[1rem] leading-snug">
              <li className="flex gap-3">
                <span className="numerals text-[11px] text-[var(--color-ink-faded)]">01.</span>
                <span>Why your eBay statement is wrong by 2.3% — and how to find it</span>
              </li>
              <li className="flex gap-3">
                <span className="numerals text-[11px] text-[var(--color-ink-faded)]">02.</span>
                <span>The four fee categories you're definitely losing money on</span>
              </li>
              <li className="flex gap-3">
                <span className="numerals text-[11px] text-[var(--color-ink-faded)]">03.</span>
                <span>Repricer wars: what shaved 6.1% off our margin last quarter</span>
              </li>
              <li className="flex gap-3">
                <span className="numerals text-[11px] text-[var(--color-ink-faded)]">04.</span>
                <span>Sourcing radar — finding profit in your competitors' deadstock</span>
              </li>
            </ol>
          </aside>
        </div>
      </section>

      {/* ─── Live ticker ──────────────────────────────────────────── */}
      <section className="rule-h-double overflow-hidden bg-[var(--color-ink)] py-4 text-[var(--color-newsprint)]">
        <div className="flex w-max ticker">
          {[...TICKER_ITEMS, ...TICKER_ITEMS].map((item, i) => (
            <div
              key={i}
              className="flex shrink-0 items-baseline gap-3 border-r border-[var(--color-ink-soft)] px-10"
            >
              <span className="font-mono text-[10px] uppercase tracking-[0.16em] text-[var(--color-newsprint)]/60">
                {item.label}
              </span>
              <span
                className="numerals text-base font-medium"
                style={item.tone === "forest" ? { color: "#7FCBA8" } : undefined}
              >
                {item.value}
              </span>
            </div>
          ))}
        </div>
      </section>

      {/* ─── Three departments — newspaper columns ───────────────── */}
      <section className="mx-auto max-w-7xl px-6 pt-20 pb-16 md:px-10 md:pt-28">
        <div className="grid grid-cols-1 gap-px md:grid-cols-3" style={{ background: "var(--color-rule)" }}>
          {DEPARTMENTS.map((d) => (
            <article key={d.kicker} className="bg-[var(--color-newsprint)] p-8 md:p-10">
              <p className="kicker">{d.kicker}</p>
              <h2 className="mt-3 font-display text-[2rem] font-medium leading-tight tracking-tight md:text-[2.25rem]">
                {d.title}
              </h2>
              <p className="mt-4 text-[15px] leading-relaxed text-[var(--color-ink-soft)]">
                {d.lede}
              </p>
              <p className="mt-5 border-l-2 pl-4 text-[13px] italic leading-relaxed text-[var(--color-ink-faded)]"
                 style={{ borderColor: "var(--color-oxblood)" }}>
                {d.detail}
              </p>
            </article>
          ))}
        </div>
      </section>

      {/* ─── Pull quote ───────────────────────────────────────────── */}
      <section className="mx-auto max-w-5xl px-6 py-20 md:px-10 md:py-32 text-center">
        <p className="font-mono text-[11px] uppercase tracking-[0.2em] text-[var(--color-ink-faded)]">
          Editorial · The standard we hold
        </p>
        <blockquote className="mt-8 font-display text-[2.25rem] font-medium italic leading-[1.15] md:text-[3.5rem]">
          <span
            aria-hidden
            className="block font-display text-[5rem] leading-none md:text-[7rem]"
            style={{ color: "var(--color-oxblood)" }}
          >
            “
          </span>
          The number on your dashboard is the number you can{" "}
          <span style={{ color: "var(--color-oxblood)" }}>defend</span> — to your
          accountant, your spouse, and tax season.
        </blockquote>
      </section>

      {/* ─── Closing CTA ──────────────────────────────────────────── */}
      <section className="rule-h-double mx-auto max-w-7xl px-6 py-16 md:px-10">
        <div className="grid grid-cols-1 items-end gap-10 md:grid-cols-12">
          <div className="md:col-span-7">
            <p className="kicker">Subscribe</p>
            <h2 className="mt-3 font-display text-[3rem] font-medium leading-[0.95] tracking-tight md:text-[4.5rem]">
              Open an account.
              <br />
              <em
                className="not-italic"
                style={{ color: "var(--color-oxblood)", fontStyle: "italic" }}
              >
                The next 14 days are on us.
              </em>
            </h2>
          </div>
          <div className="flex flex-col gap-3 md:col-span-5 md:items-end">
            <Link to="/signup" className="btn-ink w-fit">
              Start free trial →
            </Link>
            <span className="font-mono text-[11px] text-[var(--color-ink-faded)]">
              No credit card · No data collected pre-signup · Cancel in one click
            </span>
          </div>
        </div>
      </section>

      {/* ─── Footer — bibliographic ──────────────────────────────── */}
      <footer className="border-t border-[var(--color-rule)] py-10 text-[12px] text-[var(--color-ink-faded)]">
        <div className="mx-auto flex max-w-7xl flex-col gap-4 px-6 md:flex-row md:justify-between md:px-10">
          <div className="font-mono">
            <span className="uppercase tracking-[0.18em]">The eBay-Soft Ledger</span>
            <span className="mx-3">·</span>
            <span>Vol. I · Issue 001 · {ISSUE_DATE}</span>
            <span className="mx-3">·</span>
            <span>Published in Frankfurt &amp; the cloud</span>
          </div>
          <div className="flex gap-6 font-mono uppercase tracking-[0.16em]">
            <Link to="/privacy" className="link-underline">Privacy</Link>
            <Link to="/terms" className="link-underline">Terms</Link>
            <a href="https://github.com/legisus/ebay-soft" className="link-underline">Source</a>
          </div>
        </div>
      </footer>
    </div>
  );
}
