import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./lib/auth";
import Landing from "./pages/Landing";
import Signup from "./pages/Signup";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import Privacy from "./pages/Privacy";
import Terms from "./pages/Terms";

/**
 * Authenticated users skip the landing page and go straight to /dashboard;
 * everyone else sees the marketing page at /.
 */
function RootRoute() {
  const { me, loading } = useAuth();
  if (loading) return <FullPageHint label="Loading…" />;
  return me ? <Navigate to="/dashboard" replace /> : <Landing />;
}

function RequireAuth({ children }: { children: React.ReactElement }) {
  const { me, loading } = useAuth();
  if (loading) return <FullPageHint label="Loading…" />;
  if (!me) return <Navigate to="/login" replace />;
  return children;
}

function FullPageHint({ label }: { label: string }) {
  return (
    <div className="flex min-h-screen items-center justify-center font-mono text-xs uppercase tracking-[0.16em] text-[var(--color-ink-faded)]">
      {label}
    </div>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<RootRoute />} />
      <Route path="/signup" element={<Signup />} />
      <Route path="/login" element={<Login />} />
      <Route path="/privacy" element={<Privacy />} />
      <Route path="/terms" element={<Terms />} />
      <Route
        path="/dashboard"
        element={
          <RequireAuth>
            <Dashboard />
          </RequireAuth>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
