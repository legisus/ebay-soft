import { useAuth } from "../lib/auth";

export default function Dashboard() {
  const { me, logout } = useAuth();
  if (!me) return null; // RequireAuth handles redirect; this guards the type narrow.

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

      <section className="mt-10 rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="text-base font-semibold">Connect your eBay account</h2>
        <p className="mt-2 text-sm text-slate-600">
          Connect a sandbox eBay seller account to backfill orders and start tracking profit.
          The full OAuth round-trip lands in issue #164.
        </p>
        <button
          className="btn-primary mt-4 max-w-xs"
          disabled
          aria-disabled
          title="OAuth round-trip lands in #164"
        >
          Connect eBay (coming soon)
        </button>
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
