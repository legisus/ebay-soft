import { useCallback, useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useAuth } from "../lib/auth";
import { TOKEN_STORAGE_KEY } from "../lib/api";

const NOW = new Date().toLocaleDateString("en-GB", {
  weekday: "long",
  day: "numeric",
  month: "long",
  year: "numeric",
});

interface EbayAccountView {
  id: string;
  marketplaceId: string;
  ebayUserId: string | null;
  status: string;
  connectedAt: string;
}

export default function Dashboard() {
  const { me, logout } = useAuth();
  const [params, setParams] = useSearchParams();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [accounts, setAccounts] = useState<EbayAccountView[] | null>(null);

  const ebayStatus = params.get("ebay");

  /** Fetch connected eBay accounts. Called on mount + after a successful connect. */
  const refreshAccounts = useCallback(async () => {
    if (!me) return;
    try {
      const token = localStorage.getItem(TOKEN_STORAGE_KEY);
      const r = await fetch("/v1/ebay/accounts", {
        headers: {
          "X-Tenant-Id": me.tenant_id,
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      });
      if (!r.ok) throw new Error(`HTTP ${r.status}`);
      const data = (await r.json()) as { accounts: EbayAccountView[] };
      setAccounts(data.accounts);
    } catch (e) {
      console.warn("could not load eBay accounts", e);
      setAccounts([]); // best-effort: show the empty state instead of breaking the page
    }
  }, [me]);

  useEffect(() => {
    refreshAccounts();
  }, [refreshAccounts]);

  // Banner timeout — clear ?ebay=… after 6s and refresh the accounts list on
  // a successful connect so the page reflects the new state without a manual
  // refresh.
  useEffect(() => {
    if (!ebayStatus) return;
    if (ebayStatus === "connected") refreshAccounts();
    const t = setTimeout(() => {
      params.delete("ebay");
      setParams(params, { replace: true });
    }, 6000);
    return () => clearTimeout(t);
  }, [ebayStatus, params, setParams, refreshAccounts]);

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

  const hasAccounts = (accounts?.length ?? 0) > 0;

  return (
    <div className="grain min-h-screen">
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
        <p className="kicker">Subscriber dossier</p>
        <h1 className="mt-2 font-display text-[2.5rem] font-medium leading-tight tracking-tight md:text-[3rem]">
          {me.email}
        </h1>
        <p className="mt-1 font-mono text-[12px] uppercase tracking-[0.14em] text-[var(--color-ink-faded)]">
          Tenant {me.tenant_id} · Role {me.role}
        </p>

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

        <div className="mt-12 grid grid-cols-1 gap-px md:grid-cols-2" style={{ background: "var(--color-rule)" }}>
          {/* I — Account linking. Shows the list when connected, the CTA when not. */}
          <section className="bg-[var(--color-newsprint)] p-8 md:p-10">
            <p className="kicker">I. Account linking</p>

            {accounts === null && (
              <p className="mt-4 font-mono text-[12px] uppercase tracking-[0.14em] text-[var(--color-ink-faded)]">
                Loading accounts…
              </p>
            )}

            {accounts !== null && !hasAccounts && (
              <>
                <h2 className="mt-3 font-display text-[1.75rem] font-medium leading-tight tracking-tight">
                  Connect your eBay account.
                </h2>
                <p className="mt-4 text-[15px] leading-relaxed text-[var(--color-ink-soft)]">
                  We use OAuth 2.0 on the eBay sandbox. We never see your password — only a refresh
                  token, encrypted at rest with a key we don't log.
                </p>
                <button className="btn-ink mt-6" onClick={connectEbay} disabled={busy}>
                  {busy ? "Redirecting to eBay…" : "Connect eBay →"}
                </button>
              </>
            )}

            {accounts !== null && hasAccounts && (
              <>
                <h2 className="mt-3 font-display text-[1.75rem] font-medium leading-tight tracking-tight">
                  Connected accounts
                </h2>
                <ul className="mt-6 divide-y divide-[var(--color-rule)] border-y border-[var(--color-rule)]">
                  {accounts!.map((a) => (
                    <li key={a.id} className="flex items-baseline justify-between gap-4 py-3">
                      <div>
                        <p className="font-display text-[1.05rem] leading-tight">
                          {a.ebayUserId ?? <span className="text-[var(--color-ink-faded)] italic">pending identity</span>}
                        </p>
                        <p className="mt-0.5 font-mono text-[11px] uppercase tracking-[0.14em] text-[var(--color-ink-faded)]">
                          {a.marketplaceId} · connected{" "}
                          {new Date(a.connectedAt).toLocaleDateString("en-GB", {
                            day: "numeric",
                            month: "short",
                            year: "numeric",
                          })}
                        </p>
                      </div>
                      <span
                        className="numerals text-[11px] uppercase tracking-[0.14em]"
                        style={{
                          color:
                            a.status === "connected"
                              ? "var(--color-forest)"
                              : "var(--color-oxblood)",
                        }}
                      >
                        {a.status}
                      </span>
                    </li>
                  ))}
                </ul>
                <button className="btn-ghost mt-6" onClick={connectEbay} disabled={busy}>
                  {busy ? "Redirecting to eBay…" : "Add another →"}
                </button>
              </>
            )}

            {error && (
              <p
                className="mt-4 font-mono text-[12px] uppercase tracking-[0.12em]"
                role="alert"
                style={{ color: "var(--color-oxblood)" }}
              >
                {error}
              </p>
            )}
          </section>

          {/* II — Numbers. Stays placeholder until the sync backfill (#165) + accounting consumer (#49) wire up. */}
          <section className="bg-[var(--color-newsprint)] p-8 md:p-10">
            <p className="kicker">II. Your numbers</p>
            <h2 className="mt-3 font-display text-[1.75rem] font-medium leading-tight tracking-tight text-[var(--color-ink-faded)]">
              {hasAccounts ? "Awaiting first sync." : "Connect to see numbers."}
            </h2>
            <p className="mt-4 text-[15px] leading-relaxed text-[var(--color-ink-soft)]">
              Charts and P&amp;L appear here once your eBay account is connected and the first
              backfill completes. Read-side lives at{" "}
              <code className="numerals text-[13px]">/v1/pnl</code>.
            </p>
            <dl className="mt-8 grid grid-cols-2 gap-x-6 gap-y-4">
              {[
                ["Net revenue (30d)"],
                ["Fees paid"],
                ["Orders synced"],
                ["Margin"],
              ].map(([label]) => (
                <div key={label}>
                  <dt className="font-mono text-[10px] uppercase tracking-[0.18em] text-[var(--color-ink-faded)]">
                    {label}
                  </dt>
                  <dd className="mt-1 numerals text-2xl text-[var(--color-ink-faded)]">—</dd>
                </div>
              ))}
            </dl>
          </section>
        </div>
      </main>
    </div>
  );
}
