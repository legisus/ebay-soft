/**
 * Thin REST client for the api-gateway. Returns parsed JSON; throws on non-2xx with the
 * RFC-7807 Problem Detail attached when present.
 */

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  [k: string]: unknown;
}

export class ApiError extends Error {
  constructor(public readonly status: number, public readonly problem: ProblemDetail | null) {
    super(problem?.detail ?? problem?.title ?? `HTTP ${status}`);
    this.name = "ApiError";
  }
}

export interface Money {
  amount: string;        // serialized as JSON string per docs/BACKEND.md
  currency: string;      // ISO 4217
}

function getToken(): string | null {
  return localStorage.getItem("ebay-soft.jwt");
}

export function setToken(token: string | null) {
  if (token === null) localStorage.removeItem("ebay-soft.jwt");
  else localStorage.setItem("ebay-soft.jwt", token);
}

export async function api<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const headers = new Headers(init.headers ?? {});
  headers.set("Accept", "application/json");
  if (!headers.has("Content-Type") && init.body) headers.set("Content-Type", "application/json");
  const token = getToken();
  if (token) headers.set("Authorization", `Bearer ${token}`);

  const res = await fetch(path, { ...init, headers });
  if (res.status === 204) return undefined as T;

  const text = await res.text();
  const body = text ? JSON.parse(text) : null;
  if (!res.ok) throw new ApiError(res.status, body as ProblemDetail | null);
  return body as T;
}
