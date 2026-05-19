import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useAuth } from "../lib/auth";
import { TOKEN_STORAGE_KEY } from "../lib/api";

const NOW = new Date().toLocaleDateString("en-GB", {
  weekday: "long",
  day: "numeric",
  month: "long",
  year: "numeric",
});

export default function Dashboard() {
  const { me, logout } = useAuth();
  const [params, setParams] = useSearchParams();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const ebayStatus = params.get("ebay");

  useEffect(() => {
    if (!ebayStatus) return;
    const t = setTimeout(() => {
      params.delete("ebay");
      setParams(params, { replace: true });
    }, 6000);
    return () => clearTimeout(t);
  }, [ebayStatus, params, setParams]);

  if (!me) return null;

  async function connectEbay() {
    setBusy(true);
    setError(null);
    try {
      const token = localStorage.getItem(TOKEN_STORAGE_KEY);
      const response = await fetch("/v1/oauth/ebay/start", {
        method: "POST",
        headers: {
          "X-Tenant-Id": me!.tenant_id,
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const data = (await response.json()) as { authorizeUrl: string };
      window.location.href = data.authorizeUrl;
    } catch (e) {
      console.error("connect eBay failed", e);
      setError("Could not start the eBay OAuth flow. Try again in a moment.");
      setBusy(false);
    }
  }

  return (
    <div className="grain min-h-screen">
      {/* Masthead — same shape as the landing's, but with a subscriber identity. */}
      <header className="rule-h-double">
        <div className="mx-auto flex max-w-7xl flex-wrap items-baseline justify-between gap-4 px-6 py-4 md:px-10">
          <Link to="/" className="font-display text-xl font-medium tracking-tight">
            The eBay-Soft Ledger
          </Link>
          <div className="flex items-baseline gap-6">
            <span className="hidden font-mono text-[10px] uppercase tracking-[0.18em] text-[var(--color-ink-faded)] md:inline">
              {NOW}
            </span>
            <button onClick={logout} className="text-sm link-underline text-[var(--color-ink-soft)]">
              Sign out
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-6 py-12 md:px-10 md:py-16">
        {/* Subscriber line */}
        <p className="kicker">Subscriber dossier</p>
        <h1 className="mt-2 font-display text-[2.5rem] font-medium leading-tight tracking-tight md:text-[3rem]">
          {me.email}
        </h1>
        <p className="mt-1 font-mono text-[12px] uppercase tracking-[0.14em] text-[var(--color-ink-faded)]">
          Tenant {me.tenant_id} · Role {me.role}
        </p>

        {/* Status banner — connected / error */}
        {ebayStatus === "connected" && (
          <div
            role="status"
            className="mt-10 flex items-baseline gap-4 border-y px-6 py-4 font-mono text-[12px] uppercase tracking-[0.14em]"
            style={{ borderColor: "var(--color-forest)", color: "var(--color-forest)", background: "color-mix(in oklab, var(--color-forest) 6%, transparent)" }}
          >
            <span className="text-base">✓</span>
            eBay account connected. Backfill will appear here once the sync worker lands (#165).
          </div>
        )}
        {ebayStatus === "error" && (
          <div
            role="alert"
            className="mt-10 flex items-baseline gap-4 border-y px-6 py-4 font-mono text-[12px] uppercase tracking-[0.14em]"
            style={{ borderColor: "var(--color-oxblood)", color: "var(--color-oxblood)", background: "color-mix(in oklab, var(--color-oxblood) 6%, transparent)" }}
          >
            <span className="text-base">!</span>
            Something went wrong with the eBay connection. Try again or contact support.
          </div>
        )}

        {/* Two-column editorial layout */}
        <div className="mt-12 grid grid-cols-1 gap-px md:grid-cols-2" style={{ background: "var(--color-rule)" }}>
          {/* Connect column */}
          <section className="bg-[var(--color-newsprint)] p-8 md:p-10">
            <p className="kicker">I. Account linking</p>
            <h2 className="mt-3 font-display text-[1.75rem] font-medium leading-tight tracking-tight">
              Connect your eBay account.
            </h2>
            <p className="mt-4 text-[15px] leading-relaxed text-[var(--color-ink-soft)]">
              We use OAuth 2.0 on the eBay sandbox. We never see your password — only a refresh
              token, encrypted at rest with a key we don't log.
            </p>
            <button
              className="btn-ink mt-6"
              onClick={connectEbay}
              disabled={busy}
            >
              {busy ? "Redirecting to eBay…" : "Connect eBay →"}
            </button>
            {error && (
              <p className="mt-4 font-mono text-[12px] uppercase tracking-[0.12em]" role="alert"
                 style={{ color: "var(--color-oxblood)" }}>
                {error}
              </p>
            )}
          </section>

          {/* Numbers column — placeholder until #165/#49 fills it */}
          <section className="bg-[var(--color-newsprint)] p-8 md:p-10">
            <p className="kicker">II. Your numbers</p>
            <h2 className="mt-3 font-display text-[1.75rem] font-medium leading-tight tracking-tight text-[var(--color-ink-faded)]">
              Awaiting first sync.
            </h2>
            <p className="mt-4 text-[15px] leading-relaxed text-[var(--color-ink-soft)]">
              Charts and P&amp;L appear here once your eBay account is connected and the first
              backfill completes. Read-side lives at{" "}
              <code className="numerals text-[13px]">/v1/pnl</code>.
            </p>
            <dl className="mt-8 grid grid-cols-2 gap-x-6 gap-y-4">
              <div>
                <dt className="font-mono text-[10px] uppercase tracking-[0.18em] text-[var(--color-ink-faded)]">
                  Net revenue (30d)
                </dt>
                <dd className="mt-1 numerals text-2xl text-[var(--color-ink-faded)]">—</dd>
              </div>
              <div>
                <dt className="font-mono text-[10px] uppercase tracking-[0.18em] text-[var(--color-ink-faded)]">
                  Fees paid
                </dt>
                <dd className="mt-1 numerals text-2xl text-[var(--color-ink-faded)]">—</dd>
              </div>
              <div>
                <dt className="font-mono text-[10px] uppercase tracking-[0.18em] text-[var(--color-ink-faded)]">
                  Orders synced
                </dt>
                <dd className="mt-1 numerals text-2xl text-[var(--color-ink-faded)]">—</dd>
              </div>
              <div>
                <dt className="font-mono text-[10px] uppercase tracking-[0.18em] text-[var(--color-ink-faded)]">
                  Margin
                </dt>
                <dd className="mt-1 numerals text-2xl text-[var(--color-ink-faded)]">—</dd>
              </div>
            </dl>
          </section>
        </div>
      </main>
    </div>
  );
}
