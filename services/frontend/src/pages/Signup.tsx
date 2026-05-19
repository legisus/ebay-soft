import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError } from "../lib/api";
import { useAuth } from "../lib/auth";

export default function Signup() {
  const { signup } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await signup(email, password);
      navigate("/dashboard");
    } catch (e) {
      if (e instanceof ApiError && e.status === 400) {
        setError("Email already in use, or password too short (minimum 12 characters).");
      } else {
        setError("Could not open the account. Try again in a moment.");
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="grain min-h-screen">
      <header className="rule-h-double">
        <div className="mx-auto flex max-w-7xl items-baseline justify-between px-6 py-4 md:px-10">
          <Link to="/" className="font-display text-xl font-medium tracking-tight">
            The eBay-Soft Ledger
          </Link>
          <Link to="/login" className="text-sm link-underline text-[var(--color-ink-soft)]">
            Already a subscriber? Sign in
          </Link>
        </div>
      </header>

      <main className="mx-auto grid max-w-7xl gap-12 px-6 py-16 md:grid-cols-12 md:px-10 md:py-24">
        <div className="md:col-span-7">
          <p className="kicker">Subscriber registration</p>
          <h1 className="headline-xl mt-4 text-[clamp(2.5rem,6vw,4.5rem)]">
            Open your{" "}
            <em className="not-italic italic" style={{ color: "var(--color-oxblood)" }}>
              account.
            </em>
          </h1>
          <p className="dropcap mt-8 font-display text-[1.1rem] leading-[1.55] text-[var(--color-ink-soft)] md:text-[1.2rem] md:max-w-[55ch]">
            Fourteen days, no card on file, no analytics pixel. We don't ask for anything we
            don't need to give you a real number on your dashboard within minutes of connecting
            your eBay account.
          </p>
        </div>

        <form
          onSubmit={onSubmit}
          className="rule-h-double bg-[var(--color-newsprint-dim)]/40 p-8 md:col-span-5 md:p-10"
          style={{ borderTop: "3px double var(--color-rule)", borderBottom: "3px double var(--color-rule)" }}
        >
          <div>
            <label className="label" htmlFor="email">Email</label>
            <input
              id="email"
              className="field"
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>
          <div className="mt-6">
            <label className="label" htmlFor="password">Password</label>
            <input
              id="password"
              className="field"
              type="password"
              autoComplete="new-password"
              required
              minLength={12}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            <p className="mt-2 font-mono text-[11px] uppercase tracking-[0.14em] text-[var(--color-ink-faded)]">
              Minimum 12 characters
            </p>
          </div>
          {error && (
            <p className="mt-5 font-mono text-[12px] uppercase tracking-[0.12em]" role="alert"
               style={{ color: "var(--color-oxblood)" }}>
              {error}
            </p>
          )}
          <button className="btn-primary mt-8" type="submit" disabled={busy}>
            {busy ? "Opening account…" : "Open account →"}
          </button>
          <p className="mt-6 text-center font-mono text-[11px] uppercase tracking-[0.16em] text-[var(--color-ink-faded)]">
            14-day trial · no card · cancel anytime
          </p>
        </form>
      </main>
    </div>
  );
}
