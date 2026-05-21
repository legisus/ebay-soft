import { Link, useNavigate } from "react-router-dom";
import { useEffect } from "react";
import { useAuth } from "../lib/auth";

/**
 * First-run onboarding wizard. Shows the seller their place in the three-step demo path so they
 * know what to expect next, and gives the Connect-eBay CTA a more deliberate landing than the
 * dashboard's sidebar version. Real wizard (#84 full scope) gains form-driven preferences,
 * product-mix questions, and plan-selection on the way through.
 */
export default function Onboarding() {
  const { me, loading } = useAuth();
  const navigate = useNavigate();

  // Bounce anonymous traffic back to /login so the wizard always renders against a real account.
  useEffect(() => {
    if (!loading && !me) navigate("/login");
  }, [loading, me, navigate]);

  if (loading || !me) return null;

  return (
    <div className="grain min-h-screen">
      <header className="rule-h-double">
        <div className="mx-auto flex max-w-7xl items-baseline justify-between px-6 py-4 md:px-10">
          <Link to="/" className="font-display text-xl font-medium tracking-tight">
            The eBay-Soft Ledger
          </Link>
          <Link
            to="/dashboard"
            className="text-sm link-underline text-[var(--color-ink-soft)]"
          >
            Skip to dashboard
          </Link>
        </div>
      </header>

      <main className="mx-auto max-w-3xl px-6 py-16 md:px-10 md:py-24">
        <p className="kicker">Subscriber onboarding</p>
        <h1 className="mt-3 font-display text-[clamp(2.5rem,6vw,4.5rem)] font-medium leading-tight tracking-tight">
          Three steps to your first number.
        </h1>
        <p className="mt-6 font-display text-[1.1rem] leading-[1.55] text-[var(--color-ink-soft)] md:text-[1.2rem]">
          You're signed in as {me.email}. To see real P&amp;L on your dashboard, you connect your
          eBay account, we backfill the past 24 months of orders, and the reconciler stitches in
          the fees + refunds + ad spend. Average time from here to a real number: about an hour.
        </p>

        <ol className="mt-14 space-y-px" style={{ background: "var(--color-rule)" }}>
          <Step
            n="01"
            title="Sign in"
            done
            body="Done — your tenant is provisioned and ready."
          />
          <Step
            n="02"
            title="Connect your eBay account"
            body="OAuth round-trip on the eBay sandbox. We never see your password, only an encrypted refresh token. Takes about 90 seconds."
            cta={<Link to="/dashboard" className="btn-ink">Connect eBay →</Link>}
          />
          <Step
            n="03"
            title="Watch the backfill"
            pending
            body="The sync worker pulls 24 months of orders + finance events into your tenant's schema. You can leave this tab — we'll email you when it's done."
          />
        </ol>

        <p className="mt-16 font-mono text-[11px] uppercase tracking-[0.16em] text-[var(--color-ink-faded)]">
          Questions? <a className="link-underline" href="mailto:hello@ebay-soft.com">hello@ebay-soft.com</a>
        </p>
      </main>
    </div>
  );
}

function Step({
  n,
  title,
  body,
  cta,
  done,
  pending,
}: {
  n: string;
  title: string;
  body: string;
  cta?: React.ReactNode;
  done?: boolean;
  pending?: boolean;
}) {
  const tone = done ? "var(--color-forest)" : pending ? "var(--color-ink-faded)" : "var(--color-oxblood)";
  return (
    <li className="bg-[var(--color-newsprint)] p-7 md:p-9">
      <div className="grid grid-cols-[3rem_1fr] items-baseline gap-x-5">
        <span className="numerals text-2xl" style={{ color: tone }}>
          {done ? "✓" : n}
        </span>
        <div>
          <h2 className="font-display text-[1.5rem] font-medium leading-tight tracking-tight">
            {title}
          </h2>
          <p className="mt-3 text-[15px] leading-relaxed text-[var(--color-ink-soft)]">{body}</p>
          {cta && <div className="mt-5">{cta}</div>}
        </div>
      </div>
    </li>
  );
}
