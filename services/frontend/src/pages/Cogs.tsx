import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../lib/auth";
import { TOKEN_STORAGE_KEY } from "../lib/api";

interface CogsEntry {
  skuCode: string;
  amount: string;
  currency: string;
  effectiveFrom: string;
}

/**
 * Manual COGS entry — list view + simple add form. Backs onto the endpoints landed in #54
 * (POST /v1/cogs, GET /v1/cogs, DELETE /v1/cogs/{sku}/{effectiveFrom}). Page exists so a
 * tenant without inventory-api yet can still seed costs against orders that already synced.
 */
export default function Cogs() {
  const { me } = useAuth();
  const [rows, setRows] = useState<CogsEntry[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState({ skuCode: "", amount: "", currency: "USD", effectiveFrom: "" });
  const [busy, setBusy] = useState(false);

  const auth = useCallback(() => {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    return {
      "X-Tenant-Id": me?.tenant_id ?? "",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    };
  }, [me]);

  const reload = useCallback(async () => {
    if (!me) return;
    try {
      const r = await fetch("/v1/cogs", { headers: auth() });
      if (!r.ok) throw new Error(`HTTP ${r.status}`);
      setRows((await r.json()) as CogsEntry[]);
    } catch (e) {
      console.warn("COGS list failed", e);
      setRows([]);
    }
  }, [me, auth]);

  useEffect(() => {
    reload();
  }, [reload]);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const r = await fetch("/v1/cogs", {
        method: "POST",
        headers: { ...auth(), "Content-Type": "application/json" },
        body: JSON.stringify({
          skuCode: form.skuCode,
          amount: form.amount,
          currency: form.currency,
          effectiveFrom: form.effectiveFrom,
        }),
      });
      if (!r.ok) throw new Error(`HTTP ${r.status}`);
      setForm({ skuCode: "", amount: "", currency: "USD", effectiveFrom: "" });
      await reload();
    } catch (e) {
      console.error(e);
      setError("Could not save the entry.");
    } finally {
      setBusy(false);
    }
  }

  async function remove(sku: string, effectiveFrom: string) {
    if (!window.confirm(`Remove COGS for ${sku} effective ${effectiveFrom}?`)) return;
    try {
      const r = await fetch(`/v1/cogs/${encodeURIComponent(sku)}/${effectiveFrom}`, {
        method: "DELETE",
        headers: auth(),
      });
      if (!r.ok) throw new Error(`HTTP ${r.status}`);
      await reload();
    } catch (e) {
      console.error(e);
      setError("Could not remove the entry.");
    }
  }

  if (!me) return null;

  return (
    <div className="grain min-h-screen">
      <header className="rule-h-double">
        <div className="mx-auto flex max-w-7xl items-baseline justify-between px-6 py-4 md:px-10">
          <Link to="/" className="font-display text-xl font-medium tracking-tight">
            The eBay-Soft Ledger
          </Link>
          <Link to="/dashboard" className="text-sm link-underline text-[var(--color-ink-soft)]">
            ← Dashboard
          </Link>
        </div>
      </header>

      <main className="mx-auto max-w-5xl px-6 py-12 md:px-10 md:py-16">
        <p className="kicker">Section · Cost of goods sold</p>
        <h1 className="mt-2 font-display text-[2.5rem] font-medium leading-tight tracking-tight md:text-[3rem]">
          COGS entries
        </h1>
        <p className="mt-3 max-w-[60ch] text-[15px] leading-relaxed text-[var(--color-ink-soft)]">
          Per-SKU costs the P&amp;L calculation uses. History is preserved — adding a new row for
          the same SKU with a later <code>effective_from</code> takes over on orders from that
          date forward.
        </p>

        <div
          className="mt-10 grid gap-px md:grid-cols-[1fr_2fr]"
          style={{ background: "var(--color-rule)" }}
        >
          {/* Add form */}
          <section className="bg-[var(--color-newsprint)] p-7 md:p-9">
            <p className="kicker">Add</p>
            <form onSubmit={onSubmit} className="mt-5 space-y-4">
              <div>
                <label className="label" htmlFor="skuCode">SKU code</label>
                <input
                  id="skuCode" className="field" required
                  value={form.skuCode}
                  onChange={(e) => setForm({ ...form, skuCode: e.target.value })}
                />
              </div>
              <div>
                <label className="label" htmlFor="amount">Cost</label>
                <input
                  id="amount" className="field" type="number" step="0.01" required min="0"
                  value={form.amount}
                  onChange={(e) => setForm({ ...form, amount: e.target.value })}
                />
              </div>
              <div>
                <label className="label" htmlFor="currency">Currency</label>
                <input
                  id="currency" className="field" required maxLength={3} minLength={3}
                  value={form.currency}
                  onChange={(e) => setForm({ ...form, currency: e.target.value.toUpperCase() })}
                />
              </div>
              <div>
                <label className="label" htmlFor="effectiveFrom">Effective from</label>
                <input
                  id="effectiveFrom" className="field" type="date" required
                  value={form.effectiveFrom}
                  onChange={(e) => setForm({ ...form, effectiveFrom: e.target.value })}
                />
              </div>
              {error && (
                <p className="font-mono text-[12px] uppercase tracking-[0.12em]"
                   style={{ color: "var(--color-oxblood)" }} role="alert">{error}</p>
              )}
              <button className="btn-primary" type="submit" disabled={busy}>
                {busy ? "Saving…" : "Save entry"}
              </button>
            </form>
          </section>

          {/* List */}
          <section className="bg-[var(--color-newsprint)] p-7 md:p-9">
            <p className="kicker">Existing</p>
            {rows === null && (
              <p className="mt-4 font-mono text-[12px] uppercase tracking-[0.14em] text-[var(--color-ink-faded)]">
                Loading…
              </p>
            )}
            {rows !== null && rows.length === 0 && (
              <p className="mt-4 text-[15px] leading-relaxed text-[var(--color-ink-faded)]">
                No COGS entries yet. Add one with the form on the left.
              </p>
            )}
            {rows !== null && rows.length > 0 && (
              <table className="mt-4 w-full text-[14px]">
                <thead className="font-mono text-[10px] uppercase tracking-[0.16em] text-[var(--color-ink-faded)]">
                  <tr>
                    <th className="py-2 text-left">SKU</th>
                    <th className="py-2 text-right">Cost</th>
                    <th className="py-2 text-left pl-4">From</th>
                    <th className="py-2"></th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--color-rule)]">
                  {rows.map((r) => (
                    <tr key={`${r.skuCode}-${r.effectiveFrom}`} className="align-baseline">
                      <td className="py-3 font-display">{r.skuCode}</td>
                      <td className="py-3 numerals text-right">
                        {r.amount} <span className="text-[var(--color-ink-faded)] text-[11px]">{r.currency}</span>
                      </td>
                      <td className="py-3 pl-4 font-mono text-[12px] text-[var(--color-ink-soft)]">
                        {r.effectiveFrom}
                      </td>
                      <td className="py-3 text-right">
                        <button
                          onClick={() => remove(r.skuCode, r.effectiveFrom)}
                          className="font-mono text-[11px] uppercase tracking-[0.12em] text-[var(--color-oxblood)] hover:underline"
                        >
                          Remove
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </section>
        </div>
      </main>
    </div>
  );
}
