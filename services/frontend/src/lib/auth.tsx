import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { api, ApiError, TOKEN_STORAGE_KEY } from "./api";

interface Me {
  id: string;
  tenant_id: string;
  email: string;
  role: string;
}

interface AuthContextValue {
  me: Me | null;
  loading: boolean;
  signup: (email: string, password: string) => Promise<void>;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

const REFRESH_STORAGE_KEY = "ebay-soft.refresh_token";

interface TokenResponse {
  access_token: string;
  refresh_token: string;
  tenant_id: string;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [me, setMe] = useState<Me | null>(null);
  const [loading, setLoading] = useState(true);

  // On first mount, if we have a token, try to load the user. This is the
  // "remember me" path on a page reload.
  useEffect(() => {
    let cancelled = false;
    (async () => {
      const token = localStorage.getItem(TOKEN_STORAGE_KEY);
      if (!token) {
        setLoading(false);
        return;
      }
      try {
        const profile = await api.get<Me>("/v1/auth/me");
        if (!cancelled) setMe(profile);
      } catch (e) {
        if (e instanceof ApiError && e.status === 401) {
          // api.ts already cleared the token; nothing else to do.
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const persistTokensAndLoadMe = useCallback(async (tokens: TokenResponse) => {
    localStorage.setItem(TOKEN_STORAGE_KEY, tokens.access_token);
    localStorage.setItem(REFRESH_STORAGE_KEY, tokens.refresh_token);
    const profile = await api.get<Me>("/v1/auth/me");
    setMe(profile);
  }, []);

  const signup = useCallback(
    async (email: string, password: string) => {
      const tokens = await api.post<TokenResponse>("/v1/auth/signup", { email, password });
      await persistTokensAndLoadMe(tokens);
    },
    [persistTokensAndLoadMe],
  );

  const login = useCallback(
    async (email: string, password: string) => {
      const tokens = await api.post<TokenResponse>("/v1/auth/login", { email, password });
      await persistTokensAndLoadMe(tokens);
    },
    [persistTokensAndLoadMe],
  );

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(REFRESH_STORAGE_KEY);
    setMe(null);
  }, []);

  const value = useMemo(() => ({ me, loading, signup, login, logout }), [me, loading, signup, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside an AuthProvider");
  return ctx;
}
