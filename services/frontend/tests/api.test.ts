import { describe, expect, it, vi } from "vitest";
import { api, ApiError, TOKEN_STORAGE_KEY } from "../src/lib/api";

describe("api client", () => {
  it("attaches the bearer token from localStorage on GET", async () => {
    localStorage.setItem(TOKEN_STORAGE_KEY, "stored-token");
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(new Response(JSON.stringify({ ok: true }), { status: 200 }));

    await api.get("/v1/auth/me");

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [, init] = fetchMock.mock.calls[0];
    const headers = new Headers(init?.headers);
    expect(headers.get("Authorization")).toBe("Bearer stored-token");
  });

  it("sends JSON content-type and the body on POST", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(
        new Response(JSON.stringify({ tenant_id: "t-1" }), {
          status: 201,
          headers: { "Content-Type": "application/json" },
        }),
      );

    const result = await api.post<{ tenant_id: string }>("/v1/auth/signup", {
      email: "a@b.com",
      password: "pw",
    });

    expect(result.tenant_id).toBe("t-1");
    const [, init] = fetchMock.mock.calls[0];
    expect(init?.method).toBe("POST");
    expect(new Headers(init?.headers).get("Content-Type")).toBe("application/json");
    expect(init?.body).toBe(JSON.stringify({ email: "a@b.com", password: "pw" }));
  });

  it("clears the stored token and throws ApiError on 401", async () => {
    localStorage.setItem(TOKEN_STORAGE_KEY, "expired-token");
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(null, { status: 401 }),
    );

    await expect(api.get("/v1/auth/me")).rejects.toBeInstanceOf(ApiError);
    expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull();
  });

  it("throws ApiError with the status for non-2xx responses", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ detail: "nope" }), { status: 400 }),
    );

    await expect(api.post("/v1/auth/signup", {})).rejects.toMatchObject({
      status: 400,
    });
  });
});
