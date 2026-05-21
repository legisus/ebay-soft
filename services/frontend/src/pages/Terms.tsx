import { Link } from "react-router-dom";

const EFFECTIVE = new Date().toLocaleDateString("en-GB", {
  day: "numeric",
  month: "long",
  year: "numeric",
});

/**
 * Terms of service. Draft — replace before any non-allowlisted signup.
 */
export default function Terms() {
  return (
    <div className="grain min-h-screen">
      <header className="rule-h-double">
        <div className="mx-auto flex max-w-7xl items-baseline justify-between px-6 py-4 md:px-10">
          <Link to="/" className="font-display text-xl font-medium tracking-tight">
            The eBay-Soft Ledger
          </Link>
          <span className="font-mono text-[10px] uppercase tracking-[0.18em] text-[var(--color-ink-faded)]">
            Terms · Effective {EFFECTIVE}
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
          Terms of service.
        </h1>
        <p className="dropcap mt-8 font-display text-[1.1rem] leading-[1.55] text-[var(--color-ink-soft)]">
          By creating an account you agree to use eBay-Soft within the limits described here. The
          short version is the obvious one: pay if you're on a paid plan, don't try to break the
          service, eBay's rules still apply to the seller account you connect. The long form is
          below.
        </p>

        <Section title="The service">
          <p className="text-[15px] leading-relaxed text-[var(--color-ink-soft)]">
            eBay-Soft is a software-as-a-service product that reads data from your eBay seller
            account on your behalf and presents it as profit-and-loss reporting, inventory views,
            and related analytics. The eBay APIs we use are owned by eBay; their availability and
            rate limits are out of our control.
          </p>
        </Section>

        <Section title="Your account">
          <Item label="Eligibility">
            You must be at least 18 years old and have a legitimate eBay seller account.
          </Item>
          <Item label="Truthful info">
            The email you sign up with must be your own and reachable. We may suspend accounts that
            provide false contact information.
          </Item>
          <Item label="One per seller">
            One eBay-Soft account per eBay seller account. We may merge or close duplicate accounts.
          </Item>
        </Section>

        <Section title="Acceptable use">
          <Item label="No reselling">
            You may not resell access to our service. Each seat is for one operator and their team.
          </Item>
          <Item label="No abuse">
            No scraping, no automated load that's not a fair use of the documented APIs, no attempts
            to access another tenant's data.
          </Item>
          <Item label="eBay's rules">
            Your eBay account remains subject to eBay's policies. We don't help you violate them, and
            we may disable our access to your account if eBay flags a breach.
          </Item>
        </Section>

        <Section title="Pricing">
          <p className="text-[15px] leading-relaxed text-[var(--color-ink-soft)]">
            Free during the demo period. Paid plans land alongside billing in a later release. We
            will notify subscribers a minimum of 30 days before any plan transition, and will not
            charge a card you didn't enter yourself.
          </p>
        </Section>

        <Section title="Data + warranties">
          <p className="text-[15px] leading-relaxed text-[var(--color-ink-soft)]">
            eBay-Soft is provided <em>as is</em>. We make a real effort to reconcile your eBay data
            accurately, but the number on your dashboard is for informational purposes — you remain
            responsible for filing your own taxes from your eBay-issued statements. Use our exports
            as a check, not as a substitute.
          </p>
        </Section>

        <Section title="Termination">
          <p className="text-[15px] leading-relaxed text-[var(--color-ink-soft)]">
            You can close your account at any time. We can suspend an account that's abusing the
            service or breaching these terms after notice. On termination we keep your data for 30
            days (for accidental-delete recovery) and then permanently remove it.
          </p>
        </Section>

        <Section title="Changes">
          <p className="text-[15px] leading-relaxed text-[var(--color-ink-soft)]">
            We may revise these terms; material changes are announced at least 14 days before they
            take effect via the email on file. Continuing to use the service after that constitutes
            acceptance.
          </p>
        </Section>

        <p className="mt-16 font-mono text-[11px] uppercase tracking-[0.16em] text-[var(--color-ink-faded)]">
          <Link to="/" className="link-underline">← Back to the front page</Link>
          <span className="mx-3">·</span>
          <Link to="/privacy" className="link-underline">Privacy policy</Link>
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
