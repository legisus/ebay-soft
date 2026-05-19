import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./lib/auth";
import Signup from "./pages/Signup";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";

function RequireAuth({ children }: { children: React.ReactElement }) {
  const { me, loading } = useAuth();
  if (loading) return <FullPageHint label="Loading…" />;
  if (!me) return <Navigate to="/login" replace />;
  return children;
}

function FullPageHint({ label }: { label: string }) {
  return (
    <div className="flex min-h-screen items-center justify-center text-sm text-slate-600">
      {label}
    </div>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="/signup" element={<Signup />} />
      <Route path="/login" element={<Login />} />
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
