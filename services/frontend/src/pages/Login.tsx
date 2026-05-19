import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError } from "../lib/api";
import { useAuth } from "../lib/auth";

export default function Login() {
  const { login } = useAuth();
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
      await login(email, password);
      navigate("/dashboard");
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) {
        setError("Email and password don't match our records.");
      } else {
        setError("Could not sign in. Try again in a moment.");
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
          <Link to="/signup" className="text-sm link-underline text-[var(--color-ink-soft)]">
            New here? Open an account
          </Link>
        </div>
      </header>

      <main className="mx-auto max-w-md px-6 py-20 md:py-32">
        <p className="kicker text-center">Subscriber sign-in</p>
        <h1 className="mt-3 text-center font-display text-[2.75rem] font-medium leading-tight tracking-tight">
          Welcome back.
        </h1>

        <form
          onSubmit={onSubmit}
          className="mt-12 p-8"
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
              autoComplete="current-password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          {error && (
            <p className="mt-5 font-mono text-[12px] uppercase tracking-[0.12em]" role="alert"
               style={{ color: "var(--color-oxblood)" }}>
              {error}
            </p>
          )}
          <button className="btn-primary mt-8" type="submit" disabled={busy}>
            {busy ? "Signing in…" : "Sign in →"}
          </button>
        </form>

        <p className="mt-8 text-center font-mono text-[11px] uppercase tracking-[0.16em] text-[var(--color-ink-faded)]">
          <Link to="/signup" className="link-underline">Open a new account</Link>
        </p>
      </main>
    </div>
  );
}
