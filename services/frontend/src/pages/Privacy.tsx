import { Link } from "react-router-dom";

const EFFECTIVE = new Date().toLocaleDateString("en-GB", {
  day: "numeric",
  month: "long",
  year: "numeric",
});

/**
 * Privacy policy. Written as a clearly-labelled DRAFT — not yet reviewed by
 * counsel, must be replaced before any non-allowlisted user can sign up.
 * Content describes what the platform actually does today (no analytics,
 * encrypted refresh tokens, etc.), not aspirational compliance language.
 */
export default function Privacy() {
  return (
    <div className="grain min-h-screen">
      <header className="rule-h-double">
        <div className="mx-auto flex max-w-7xl items-baseline justify-between px-6 py-4 md:px-10">
          <Link to="/" className="font-display text-xl font-medium tracking-tight">
            The eBay-Soft Ledger
          </Link>
          <span className="font-mono text-[10px] uppercase tracking-[0.18em] text-[var(--color-ink-faded)]">
            Privacy · Effective {EFFECTIVE}
          </span>
        </div>
      </header>

      <main className="mx-auto max-w-3xl px-6 py-16 md:px-10 md:py-24">
        <div
          className="mb-10 border-l-2 px-5 py-3 font-mono text-[12px] uppercase tracking-[0.14em]"
          style={{ borderColor: "var(--color-oxblood)", color: "var(--color-oxblood)" }}
        >
          Draft · Not yet reviewed by counsel · Pre-launch placeholder
        </div>

        <p className="kicker">Editorial</p>
        <h1 className="mt-3 font-display text-[3rem] font-medium leading-tight tracking-tight md:text-[4rem]">
          Privacy.
        </h1>
        <p className="dropcap mt-8 font-display text-[1.1rem] leading-[1.55] text-[var(--color-ink-soft)]">
          eBay-Soft connects to your eBay seller account to read orders, listings, and finance
          events on your behalf. This page explains what we collect, what we don't, and where it
          lives. Plain English up top, the long form below.
        </p>

        <section className="mt-12">
          <h2 className="font-display text-2xl font-medium">In one sentence</h2>
          <p className="mt-3 text-[15px] leading-relaxed text-[var(--color-ink-soft)]">
            We collect the email + password you sign up with, the OAuth refresh token eBay hands us
            when you connect your seller account, and the orders + finance events we pull from
            eBay so we can compute your P&amp;L. We don't run analytics. We don't share data with
            third parties. You can delete your account at any time.
          </p>
        </section>

        <Section title="What we collect">
          <Item label="Account">
            Your email address and an Argon2id-hashed password. We never see your password in
            plaintext after submission.
          </Item>
          <Item label="eBay tokens">
            When you click <em>Connect eBay</em> and authorize on eBay's domain, eBay returns a
            refresh token. We encrypt it at rest with AES-256-GCM using a key kept outside the
            database, and decrypt it only inside our own outbound calls to eBay.
          </Item>
          <Item label="Order + finance data">
            Order IDs, item totals, fees, refunds, payouts, and the timestamps thereof — pulled
            from eBay's Fulfillment + Finances APIs. Stored per-tenant in a Postgres schema isolated
            from other tenants.
          </Item>
          <Item label="Operational logs">
            Standard request logs (timestamps, status codes, request IDs) for 30 days. No bodies, no
            tokens. Used to debug your account when you ask for support.
          </Item>
        </Section>

        <Section title="What we don't collect">
          <Item label="Behavioral analytics">No third-party tracker, no session replay, no heatmap.</Item>
          <Item label="Marketing pixels">Zero. The HTML you see has no <code>img</code> beacons.</Item>
          <Item label="Cookies">A single session cookie for signed-in state. No third-party cookies.</Item>
        </Section>

        <Section title="Where it lives">
          <p className="text-[15px] leading-relaxed text-[var(--color-ink-soft)]">
            Servers are in Helsinki, Finland (Hetzner). Data is replicated nightly to encrypted
            offsite backups. We don't move your data outside the EU. We don't sell access to any
            third party. The only outbound calls we make on your behalf are to eBay (to read your
            data) and to Postmark/SES (only after the email-verification flow ships).
          </p>
        </Section>

        <Section title="Your rights">
          <p className="text-[15px] leading-relaxed text-[var(--color-ink-soft)]">
            You can export everything we have on you (GET <code>/v1/me/export</code> when the
            endpoint ships), delete your account along with every row tied to it (a 30-day grace
            period for accidental deletes), and revoke our eBay access at any time from your eBay
            account settings — which immediately invalidates the refresh token we hold.
          </p>
        </Section>

        <Section title="Contact">
          <p className="text-[15px] leading-relaxed text-[var(--color-ink-soft)]">
            Privacy questions: <a className="link-underline" href="mailto:privacy@ebay-soft.com">privacy@ebay-soft.com</a>.
            Security disclosures: <a className="link-underline" href="mailto:security@ebay-soft.com">security@ebay-soft.com</a>.
            We aim to respond inside two business days.
          </p>
        </Section>

        <p className="mt-16 font-mono text-[11px] uppercase tracking-[0.16em] text-[var(--color-ink-faded)]">
          <Link to="/" className="link-underline">← Back to the front page</Link>
          <span className="mx-3">·</span>
          <Link to="/terms" className="link-underline">Terms of service</Link>
        </p>
      </main>
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="mt-12">
      <h2 className="font-display text-2xl font-medium">{title}</h2>
      <div className="mt-4 space-y-4">{children}</div>
    </section>
  );
}

function Item({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="grid grid-cols-[8rem_1fr] items-baseline gap-x-4">
      <p className="kicker">{label}</p>
      <p className="text-[15px] leading-relaxed text-[var(--color-ink-soft)]">{children}</p>
    </div>
  );
}
