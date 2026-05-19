// Minimal fetch wrapper. Token lives in localStorage; auto-attached on every
// request and auto-cleared on 401 (so a stale session can't keep retrying with
// a dead JWT). Production-grade hardening (CSRF, request-id correlation,
// idempotency-key) lands when #161 ships.

export const TOKEN_STORAGE_KEY = "ebay-soft.access_token";

export class ApiError extends Error {
  readonly status: number;
  readonly body: unknown;
  constructor(status: number, body: unknown) {
    super(`API ${status}`);
    this.status = status;
    this.body = body;
  }
}

function readToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_STORAGE_KEY);
  } catch {
    return null;
  }
}

async function request<T>(
  path: string,
  init: RequestInit & { json?: unknown } = {},
): Promise<T> {
  const headers = new Headers(init.headers);
  const token = readToken();
  if (token) headers.set("Authorization", `Bearer ${token}`);

  let body: BodyInit | null | undefined = init.body;
  if (init.json !== undefined) {
    headers.set("Content-Type", "application/json");
    body = JSON.stringify(init.json);
  }

  const response = await fetch(path, { ...init, headers, body });

  if (response.status === 401) {
    try {
      localStorage.removeItem(TOKEN_STORAGE_KEY);
    } catch {
      /* ignore */
    }
    throw new ApiError(401, await safeJson(response));
  }
  if (!response.ok) {
    throw new ApiError(response.status, await safeJson(response));
  }
  // 204 / empty body: return undefined cast to T.
  const text = await response.text();
  return text.length === 0 ? (undefined as T) : (JSON.parse(text) as T);
}

async function safeJson(response: Response): Promise<unknown> {
  try {
    return await response.clone().json();
  } catch {
    return null;
  }
}

export const api = {
  get: <T>(path: string) => request<T>(path, { method: "GET" }),
  post: <T>(path: string, json: unknown) => request<T>(path, { method: "POST", json }),
};
