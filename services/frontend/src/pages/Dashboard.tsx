import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useAuth } from "../lib/auth";
import { TOKEN_STORAGE_KEY } from "../lib/api";

export default function Dashboard() {
  const { me, logout } = useAuth();
  const [params, setParams] = useSearchParams();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const ebayStatus = params.get("ebay"); // 'connected' | 'error' | null

  // Clear the ?ebay=… query param a few seconds after we've shown the banner so
  // a refresh doesn't keep re-showing it.
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
    <div className="mx-auto mt-12 max-w-3xl px-6">
      <header className="flex items-center justify-between">
        <div>
          <p className="text-xs uppercase tracking-wide text-slate-500">Signed in as</p>
          <h1 className="text-lg font-semibold">{me.email}</h1>
          <p className="text-xs text-slate-500">tenant {me.tenant_id}</p>
        </div>
        <button className="btn-link" onClick={logout}>
          Sign out
        </button>
      </header>

      {ebayStatus === "connected" && (
        <div
          role="status"
          className="mt-6 rounded-md border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800"
        >
          ✓ eBay account connected. Backfill will appear here once issue #165 lands.
        </div>
      )}
      {ebayStatus === "error" && (
        <div
          role="alert"
          className="mt-6 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
        >
          Something went wrong with the eBay connection. Try again or contact support.
        </div>
      )}

      <section className="mt-10 rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="text-base font-semibold">Connect your eBay account</h2>
        <p className="mt-2 text-sm text-slate-600">
          Connect a sandbox eBay seller account to backfill orders and start tracking profit.
        </p>
        <button
          className="btn-primary mt-4 max-w-xs"
          onClick={connectEbay}
          disabled={busy}
        >
          {busy ? "Redirecting to eBay…" : "Connect eBay"}
        </button>
        {error && (
          <p className="mt-3 text-sm text-red-600" role="alert">
            {error}
          </p>
        )}
      </section>

      <section className="mt-6 rounded-lg border border-dashed border-slate-300 p-6">
        <h2 className="text-base font-semibold text-slate-500">Your numbers</h2>
        <p className="mt-2 text-sm text-slate-500">
          Charts and P&amp;L appear here once your eBay account is connected and the first
          backfill completes. See <code>/v1/pnl</code> for the read endpoint.
        </p>
      </section>
    </div>
  );
}
