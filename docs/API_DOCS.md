# API documentation (OpenAPI / Swagger)

How we author, host, version, and consume the REST APIs for ebay-soft's 12 services. The contract is the source of truth — every service publishes its own OpenAPI 3.1 spec, and every consumer (SPA, other service, future public-API user) reads it.

The contract's *role* in the architecture (single source of truth, breaking-change detection) is in [ARCHITECTURE.md](ARCHITECTURE.md); this doc is about the **developer experience** around the contract — Swagger UI, client generation, the public developer portal, versioning policy.

---

## Two audiences, two ways the spec is consumed

| Audience | Where they read the spec | What they get |
|---|---|---|
| **Internal engineers** (us) building a service or its consumer | `https://<service>.ebay-soft.dev/swagger-ui/` (dev/stg only) or the file in git at `services/<name>/openapi.yaml` | Swagger UI for try-it-now, generated Java/TS clients for actual consumption |
| **External API consumers** (Y1 H2 — power-user customers, partners, Zapier integration authors) | `https://docs.ebay-soft.com/api/` | Redoc-rendered, branded, searchable; OpenAPI YAML downloadable for client generation |

Internal engineers care about per-service detail and want to poke endpoints with bearer tokens. External developers want a unified, navigable, marketing-grade reference. We use different renderers for each — same source spec.

---

## Authoring — springdoc-openapi

Every Java service has springdoc-openapi wired in via `libs/common-web`:

```kotlin
// libs/common-web/build.gradle.kts
api("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
// (the webflux variant is added by the WebFlux services that need it)
```

The result, at runtime:

| URL | What |
|---|---|
| `GET /v3/api-docs` | OpenAPI 3.1 JSON (machine-readable) |
| `GET /v3/api-docs.yaml` | OpenAPI 3.1 YAML |
| `GET /swagger-ui/index.html` | Swagger UI for human exploration |

We do **not** expose Swagger UI in prod by default — only in `dev` and `stg`. In `prod` the spec endpoint stays public (it's the contract; we want customers to read it), but Swagger UI redirects to the unified portal at `docs.ebay-soft.com`.

### Spec-first or code-first?

**Code-first** for the services. Java is the source of truth; annotations + DTO shapes generate the spec.

- `@Operation`, `@ApiResponse`, `@Schema` annotations on every controller method and DTO
- DTOs use Java `record`s with `jakarta.validation` constraints — springdoc reads both for the schema
- A handwritten `services/<name>/openapi.yaml` is checked in to git for **review and diff**, but it's the output of a build step (`./gradlew :services:auth-api:generateOpenApi`), not the input.

CI guards against drift:

```bash
./gradlew :services:auth-api:generateOpenApi
git diff --exit-code services/auth-api/openapi.yaml
# Fails the build if the running app's spec doesn't match what's committed.
```

This is the same `openapi-diff` check referenced in [ENVIRONMENTS.md](ENVIRONMENTS.md) and [TESTING.md](TESTING.md).

### Annotations conventions

```java
@RestController
@RequestMapping("/v1/pnl")
@Tag(name = "P&L", description = "Profit & loss reports and exports")
public class PnlController {

    @GetMapping
    @Operation(
        summary = "Get P&L aggregates",
        description = "Returns net-income broken down by the requested grouping over the given date range."
    )
    @ApiResponse(responseCode = "200", description = "P&L returned",
                 content = @Content(schema = @Schema(implementation = PnlDto.class)))
    @ApiResponse(responseCode = "400", description = "Invalid date range",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public PnlDto get(
        @Parameter(description = "Inclusive start date (ISO-8601)", example = "2026-01-01")
        @RequestParam LocalDate from,
        @Parameter(description = "Exclusive end date (ISO-8601)", example = "2026-02-01")
        @RequestParam LocalDate to,
        @Parameter(description = "Grouping dimension")
        @RequestParam(defaultValue = "day") PnlGrouping groupBy
    ) { ... }
}
```

A linter (Spectral) runs in CI to enforce that every operation has a `summary`, every parameter has a `description`, and every error response references the standard `ProblemDetail` schema. Build fails on warnings.

### Money in the spec

Money fields are `type: string, format: decimal`, not numbers — see [BACKEND.md → Money handling](BACKEND.md#money-handling) for the why. A custom springdoc `ModelConverter` in `libs/common-web` ensures every `Money` field renders this way automatically:

```yaml
Money:
  type: object
  properties:
    amount:
      type: string
      format: decimal
      example: "1243.59"
    currency:
      type: string
      minLength: 3
      maxLength: 3
      example: "USD"
  required: [amount, currency]
```

The TypeScript client generated from this gets `amount: string` — no IEEE-754 round-trip risk.

---

## Hosting — internal Swagger UI

Per-service Swagger UI on dev and stg, gated by Cloudflare Access + a `developer` claim on the JWT.

```
dev:  http://localhost:8081/swagger-ui/  (auth-api on a developer's laptop)
stg:  https://auth-api.staging.ebay-soft.com/swagger-ui/
prod: redirected to docs.ebay-soft.com
```

The gateway aggregates `/v3/api-docs` from every service into a combined spec served at `GET /api/openapi.json` (also gated in dev/stg). Useful for tooling that wants "all of ebay-soft's API in one file."

---

## Hosting — public developer portal (Y1 H2)

When the public REST API ships (Y1 H2, M12 per [ROADMAP.md](ROADMAP.md)), the developer portal goes live at `docs.ebay-soft.com/api/`.

### Renderer: Redoc

Redoc beats Swagger UI for read-only public docs:

- Three-pane layout (nav, content, code samples) — better for reading
- Code samples in 6+ languages (auto-generated)
- Built-in search
- Markdown-friendly descriptions
- No "Try it" buttons by default (we add a "Try in Postman" link instead — safer for a public API)
- Static HTML output — hostable on any web server, even Cloudflare Pages

The portal is a static site (Astro + Redoc component) built once per release, deployed to `docs.ebay-soft.com`. Source spec is the **aggregated** spec served by `api-gateway`, exported at build time.

### Considered, rejected

- **Stoplight Elements** — same idea as Redoc, less customizable for our brand.
- **ReadMe.com** — paid (~$99/mo+), beautiful, but locks the docs into a vendor.
- **Bump.sh** — promising, paid, watch for later.
- **Mintlify** — modern, gorgeous, AI-search; paid, watch list.
- **Swagger UI standalone** — fine internally; too try-it-now for a public-facing surface.

### Structure of the public portal

```
docs.ebay-soft.com/
├── /                        # landing, three big links: Getting started / API reference / Webhooks
├── /api/                    # Redoc, aggregated spec
├── /webhooks/               # human-written reference for outbound webhook events (sale.created, etc.)
├── /guides/                 # how-to articles: "Import COGS in bulk", "Run repricer dry-run via API"
├── /errors/                 # human-readable reference for every ProblemDetail title we emit
└── /changelog/              # auto-generated from release tags + conventional commits
```

The landing page links to:
- The OpenAPI spec download (`openapi.yaml` and `openapi.json`)
- A Postman collection (auto-generated from the spec by `openapi-to-postmanv2`)
- The TypeScript SDK on npm (`@ebay-soft/sdk`) — published in Y1 H2
- The Python SDK on PyPI (`ebay-soft-sdk`) — published in Y1 H2

---

## Versioning policy

All API paths are prefixed with `/v1/`. The version is in the URL, not a header — explicit, copy-pasteable, cacheable.

| Change type | Allowed in `/v1`? | Trigger |
|---|---|---|
| **Additive** — new endpoint, new optional field on request, new field on response | ✅ Yes | Minor version bump in OpenAPI `info.version`, changelog entry |
| **Required field added to request** | ❌ No | Must go in `/v2` |
| **Field removed from response, field renamed** | ❌ No | Must go in `/v2`, `/v1` retained |
| **Status code or error shape changed for the same call** | ❌ No | `/v2`, `/v1` retained |
| **Behavior change observable through a request** | ❌ No | `/v2`, `/v1` retained |

When `/v2` ships, `/v1` is supported for **at least one full quarter** before deprecation. Deprecation is announced via:

- `Deprecation: true` and `Sunset: <date>` HTTP headers on `/v1` responses
- Banner in the developer portal
- Email to every tenant with an API key calling `/v1` in the last 30 days
- Discriminator field in OpenAPI: `deprecated: true` on `/v1` operations

Pact contracts ([TESTING.md](TESTING.md)) ensure consumer code doesn't break silently when a provider ships a backward-compatible change.

### Breaking-change detection in CI

```bash
# In every PR's CI
docker run --rm -v "$PWD:/specs" tufin/oasdiff \
  diff --base /specs/openapi.main.yaml --revision /specs/openapi.head.yaml \
  --fail-on ERR
```

`oasdiff` exits non-zero if the PR introduces a breaking change against the `main`-branch spec. Override only with an explicit PR label `breaking-api-change`, which gates the merge on a second reviewer.

---

## Generated clients

We never hand-write API clients. Two pipelines.

### Java clients (consumer-side) — for service-to-service

```kotlin
// services/accounting-api/build.gradle.kts
dependencies {
    // Consumes inventory-api's REST surface
    implementation(project(":clients:inventory-api-client"))
}
```

Each `clients/<service>-api-client` Gradle subproject runs `openapi-generator-cli generate` with the `spring` template + `library: webclient` (or `restclient`), producing typed `HttpExchange` interfaces that drop into Spring 6.2's `HttpServiceProxyFactory`. The wiring is shown in [BACKEND.md → Inter-service calls](BACKEND.md).

Clients are rebuilt whenever the source service's `openapi.yaml` changes. CI uses Gradle's incremental compilation so untouched clients aren't rebuilt.

### TypeScript client for the SPA

```bash
# Generated once per build from the gateway's aggregated spec
openapi-generator-cli generate \
  -i specs/aggregated-openapi.yaml \
  -g typescript-fetch \
  -o web/src/generated/api \
  --additional-properties=supportsES6=true,modelPropertyNaming=original
```

The SPA imports from `@/generated/api`:

```ts
import { PnlApi, Money } from '@/generated/api';

const pnl = await new PnlApi(config).getPnl({ from: '2026-01-01', to: '2026-02-01' });
// pnl.totalNet.amount is `string` — see Money handling
```

The generated code goes in `.gitignore`; it's a build artifact. The CI generates it before the type-check step. Drift between server schema and SPA code is impossible — they're regenerated from the same spec on every build.

### Public TypeScript and Python SDKs (Y1 H2)

When the public REST API ships, we publish branded SDKs:

- `@ebay-soft/sdk` on npm
- `ebay-soft-sdk` on PyPI

Both generated from the aggregated spec with `openapi-generator-cli` using the `typescript-axios` and `python` templates respectively, plus a thin handwritten wrapper for ergonomics (auth helpers, pagination iterators, retry middleware). Published via GitHub Actions on each release tag.

License: Apache 2.0, in line with [GOVERNANCE.md](GOVERNANCE.md)'s open-source artifacts plan.

---

## Conventions every API follows

The portal page `docs.ebay-soft.com/api/conventions/` documents these for external developers. Internal version below.

### REST style

- Resource paths are plural nouns: `/v1/skus`, `/v1/orders`, `/v1/pnl` (P&L is the exception — it's not a collection)
- Filtering with query params: `GET /v1/orders?status=paid&placed_after=2026-01-01`
- Pagination: cursor-based, `?cursor=<opaque>&limit=100`. Never offset-pagination for resources >1000 rows
- Sorting: `?sort=placed_at,-total_amount` (prefix `-` for descending)
- Sparse fields: not supported at MVP (over-engineering)
- IDs: UUIDs everywhere, `id` field in responses

### Errors — RFC 7807 Problem Details

Every error response is `application/problem+json`:

```json
{
  "type": "https://docs.ebay-soft.com/errors/ebay-rate-limited",
  "title": "eBay temporarily unavailable",
  "status": 503,
  "detail": "We hit eBay's daily rate limit. Sync resumes automatically in 32 minutes.",
  "instance": "/v1/sync/backfill",
  "retryAfter": 1920,
  "traceId": "abc123def456"
}
```

The `type` URL resolves to a human-readable explanation in the public portal's `/errors/` section. The `traceId` lets support correlate to a specific trace in Tempo ([OBSERVABILITY.md](OBSERVABILITY.md)).

### Idempotency

Every state-changing endpoint accepts an optional `Idempotency-Key: <uuid>` request header. If the same key is replayed within 24h, the original response is returned without re-executing. Implementation: an `idempotency_keys` table per service (or shared via Redis with a 24h TTL).

### Rate limits

Documented per endpoint in the OpenAPI extension `x-rate-limit`. Standard limits returned in `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset` response headers.

### Auth

- Internal (SPA → gateway): `Authorization: Bearer <user-JWT>`
- Service-to-service: `Authorization: Bearer <service-JWT>` (validated via JWKS — [SECURITY.md](SECURITY.md))
- Public API (Y1 H2): `Authorization: Bearer <api-key>` issued in the user portal; per-key rate limits and scope grants stored in `auth-api`

---

## Linting & CI gates

| Tool | What it catches | When |
|---|---|---|
| **Spectral** with our custom ruleset | Missing summaries, missing examples, non-`ProblemDetail` errors, kebab-case path violations | Every PR |
| **oasdiff** | Breaking changes vs `main` | Every PR |
| **openapi-generator-cli validate** | Spec syntactically valid | Every PR |
| Custom: "money fields use Money schema" | A response declaring an `amount` number instead of a `Money` ref → fail | Every PR |
| Custom: "no `String` for IDs" | A response declaring `id: string` without `format: uuid` → fail | Every PR |

Spectral ruleset lives in `infra/spectral/.spectral.yml`, source-controlled, evolved as we hit edge cases.

---

## Cross-references

- The role of OpenAPI as the inter-service contract → [ARCHITECTURE.md](ARCHITECTURE.md)
- The `*-api-client` Gradle pattern → [BACKEND.md → Inter-service calls](BACKEND.md)
- Money serialization rule (`string` not `number`) → [BACKEND.md → Money handling](BACKEND.md#money-handling)
- TypeScript stack on the SPA side → [FRONTEND.md](FRONTEND.md)
- Pact contract testing complements OpenAPI (catches semantic breaks the schema diff misses) → [TESTING.md](TESTING.md)
- License for the public SDK artifacts → [GOVERNANCE.md](GOVERNANCE.md)
- Where the public portal is hosted, traffic patterns → [INFRASTRUCTURE.md](INFRASTRUCTURE.md), [ENVIRONMENTS.md](ENVIRONMENTS.md)
