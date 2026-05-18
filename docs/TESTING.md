# Testing

The default rule: **every change ships with a test that would have failed before the change.** No test, no merge. The exceptions (typo-fix PRs, doc-only changes) are explicitly labelled in the PR.

This doc is the source of truth for *what* we test, *with what*, *where it runs*, and *what gates merges and deploys*. Per-service implementation lives next to the code (`services/<name>/src/test/`).

---

## TL;DR

- Standard test pyramid; ~45% unit + slice, ~25% integration with Testcontainers, ~15% Pact contracts, ~10% API E2E, ~5% UI E2E.
- **Frameworks**: JUnit 5 + Testcontainers + WireMock + Pact + REST-assured + Playwright + Gatling. One canonical tool per layer.
- **CI gates**: unit/slice/integration/contract on every PR; mutation testing weekly; load tests quarterly + pre-launch.
- **Test environment**: **no permanent 4th env**. Per-service tests run in CI with Testcontainers; full-stack E2E runs in **ephemeral Docker Compose environments** spun up by CI per release. `stg` is for manual QA and pre-prod smoke. `prod` gets synthetic monitoring.
- **Coverage gates**: 80% line coverage per service is the floor; below that, the build fails. Coverage isn't the goal — meaningful tests are. We measure coverage to catch unintentional gaps, not to optimize for it.

---

## The testing pyramid

```
         ▲                                 # of tests   typical runtime per service
        ▲▲▲     UI E2E (Playwright)         ~5%         not run per-commit
       ▲▲▲▲▲    API E2E (REST-assured)      ~10%        ephemeral env, 3–5 min
      ▲▲▲▲▲▲▲   Contract (Pact)             ~15%        in-CI per PR, ~30s
     ▲▲▲▲▲▲▲▲▲  Integration (Testcontainers) ~25%       in-CI per PR, ~2 min
    ▲▲▲▲▲▲▲▲▲▲▲ Unit + slice (JUnit 5)       ~45%       in-CI per PR, <30s
```

Numbers are rough targets, not policy. A service with mostly orchestration logic (e.g. `notif-api`) will skew toward integration; a service with heavy domain math (`accounting-api`) will skew toward unit. The shape varies per service; the **trend** does not — most tests are fast, few are slow.

---

## Layer-by-layer

### 1. Unit & slice tests — JUnit 5

**What:** Pure logic and single-layer Spring slices.

- **Pure unit:** `Money` arithmetic, P&L math, rule evaluation, repricer floor logic, mappers. No Spring, no I/O, no clock dependency (inject `Clock`). Each test <50ms.
- **Slice tests:** Spring's `@WebMvcTest` (controller + JSON), `@DataR2dbcTest` / `@DataJpaTest` (repository + real DB schema via Testcontainers), `@JsonTest` (Jackson config including `Money` serialization).

**Stack:** JUnit 5 (Jupiter), AssertJ, Mockito (for collaborator stubs, sparingly), Spring Boot Test slice annotations.

**Conventions:**

- `@DisplayName` on each test class and method, written in plain English ("rejects a Money sum across currencies").
- Tests are organized in `Given / When / Then` blocks with `// given:`, `// when:`, `// then:` comments. Adopted from BDD; pays off in unfamiliar code.
- Test names use `methodUnderTest_givenCondition_thenExpected` only when `@DisplayName` would be redundant.
- Money assertions use `Money.equals()` or `BigDecimal.compareTo` — **never** `BigDecimal.equals` (see [BACKEND.md → Money handling](BACKEND.md#money-handling)).
- Time-dependent code receives a `Clock` from Spring config; tests inject `Clock.fixed(...)`.

**Coverage target:** 80% line coverage per service (JaCoCo). Build fails below threshold.

### 2. Integration tests — Testcontainers + WireMock

**What:** A real Postgres, real Redis, real MinIO, real Flyway migrations — but external services (eBay, Stripe, Twilio, OpenAI) are WireMock'd. Tests the full request → controller → service → repository → DB path inside one service.

**Stack:**

- **Testcontainers** for Postgres (matching prod version 17), Redis, MinIO.
- **WireMock 3.x** standalone for external API stubs. Recordings of real (sanitized) eBay/Stripe responses are checked into `src/test/resources/wiremock/`.
- **Spring Boot Test** with `@SpringBootTest(webEnvironment = RANDOM_PORT)`.
- **`@ServiceConnection`** (Spring Boot 3.5) wires Testcontainers into Spring config — no manual JDBC URL juggling.

**Conventions:**

- Each service has a `BaseIntegrationTest` abstract class spinning up its container set once per JVM via `@Container static`. Subclasses reuse the containers — fast feedback.
- One integration test per critical flow, not per method. Don't re-test what unit tests already cover.
- DB state cleaned per test via `@Sql` scripts or a `truncate-all-tables` helper. Never rely on test ordering.
- WireMock recordings refreshed quarterly via a `scripts/record-fixtures.sh` that hits eBay Sandbox once with safe test data.

### 3. Contract tests — Pact (consumer-driven)

**What:** The safety net for independent deploys. Every consumer/provider pair has a Pact contract; if `accounting-api` changes its OpenAPI in a breaking way, `analytics-api`'s CI fails.

**Why Pact over Spring Cloud Contract:** consumer-driven contracts force consumers to declare what they actually need — providers can ship anything else without breaking anyone. Spring Cloud Contract puts the provider in charge, which inverts the dependency in the wrong direction for our service split.

**Stack:**

- **Pact JVM** for Java consumers/providers.
- **Pact JS** for the SPA as a consumer of the gateway.
- **Pact Python** for `ml-api` as a provider verified by Java consumers.
- **Pact Broker** self-hosted on the Hetzner box (single Docker container, ~100 MB RAM, $0 incremental cost). Stores contracts, verification results, and "can-i-deploy" matrices.

**Workflow:**

1. Consumer writes a contract test: "I will POST `/v1/pnl/recompute` with this shape, expect this response shape."
2. CI runs the consumer test against a Pact mock server — generates the contract `.json` and publishes it to the Pact Broker, tagged with the consumer's commit SHA.
3. Provider CI fetches all consumer contracts tagged for the next provider version, verifies them against the real provider, publishes verification result back to Broker.
4. Deploy gate: `pact-broker can-i-deploy --pacticipant <service> --to-environment prod` — only passes if every consumer has verified against the version being deployed.

**Coverage target:** 100% of consumer/provider pairs have at least one Pact. Not optional. This is the only thing standing between independent deploys and prod outages.

### 4. API E2E — REST-assured (or Karate)

**What:** Hits the gateway, runs flows that span 3+ services. No browser. Verifies the whole stack except the SPA.

**Stack:** REST-assured (Java) by default — it's familiar and integrates with JUnit. Karate is the alternative if we ever need non-Java testers writing API tests.

**Examples:**

- Sign-up → connect eBay (OAuth) → trigger backfill → poll sync status → fetch P&L → export CSV.
- Set up a repricer rule → dry-run → verify proposed changes match expected.
- Create a Stripe webhook event (test mode) → verify plan flips → verify access gate updates within 2s.
- Trigger a chargeback webhook → verify evidence packet assembly.

**Where it runs:** Ephemeral env (see below), spun up by CI per release candidate.

### 5. UI E2E — Playwright

**What:** Real browser, real SPA, real backend stack. Covers the critical user journeys, not every screen.

**Stack:** **Playwright** (TypeScript). Multi-browser (Chromium, Firefox, WebKit) — same tests, three runs.

**Why Playwright over Cypress/Selenium:**

- Native multi-tab support (we open eBay's OAuth in a popup; Cypress can't).
- Auto-waiting eliminates ~80% of flake.
- Built-in trace viewer makes failed-test forensics trivial.
- Parallelizable across workers without licensing.

**Test set (~30 scenarios, run on release candidates only):**

- Sign-up → 2FA setup (TOTP) → log in → dashboard renders
- Connect eBay (sandbox) → backfill → dashboard shows orders
- Open P&L → toggle Profit Mode → export PDF
- Create SKU → set COGS → P&L reflects COGS
- Set up repricer rule → dry-run → see proposed changes
- Stripe Checkout (test mode) → subscribe to Pro → access unlocks
- Trigger low-stock alert → email received in MailHog (test SMTP)
- Phase 8: Seller Shield buyer-risk badges render correctly; address-change banner appears

**Conventions:**

- Page Object Model: one class per logical page in `tests/e2e/pages/`.
- Selectors prefer `data-testid` attributes, never CSS classes or text content (which break with i18n).
- Each test is independent — fresh signup, fresh tenant, fresh data. No shared state between tests.
- Tests run in headless Chromium by default, full multi-browser only on `main` and release tags.

### 6. Load / performance — Gatling

**What:** Synthetic load. Identifies bottlenecks before customers do.

**Stack:** **Gatling** (Java DSL — keeps everything on one language for the backend team).

**Scenarios:**

- **Sync flood:** simulate 100 sellers backfilling 50k orders each in parallel. Watch eBay rate-limit handling, R2DBC pool saturation, sync-watermark integrity.
- **Dashboard load:** 500 concurrent active sessions hitting analytics SSE + P&L queries. Watch p95 latency, Postgres replica lag, gateway memory.
- **Repricer batch:** 10k SKUs needing reprice in a single run. Watch ebay-conn-api proxy timeouts.
- **Stripe webhook storm:** 1k subscription events in 60 seconds (Stripe replays during their own incidents). Watch idempotency and DB locks.

**Cadence:** Pre-launch full pass, then quarterly. Plus on-demand when a feature touches a hot path. Results archived in Grafana for trend tracking.

### 7. Mutation testing — weekly

**What:** Pitest mutates the code (changes `>` to `>=`, removes statements, etc.) and reruns the tests. If tests still pass with the mutation, the test is too weak.

**Cadence:** Weekly on `main`, not per-commit (slow — typically 30–60 min per service). Results in a Grafana dashboard. **Mutation score target: 70%** (lower than coverage because some mutations are equivalent or untestable).

Catches what coverage misses: a test that calls a method but doesn't assert anything meaningful.

### 8. Security tests

| Tool | What | Cadence |
|---|---|---|
| **CodeQL** | SAST (semantic code analysis) | Every push to `main` |
| **OWASP Dependency-Check** + **Renovate** + **Dependabot** | Known-CVE detection in deps | Every PR + nightly |
| **trivy** | Container image scan | Every image build; fail on HIGH/CRITICAL |
| **gitleaks** | Secret scanning | Pre-commit + per-PR + weekly full-history |
| **OWASP ZAP** | DAST against staging | Weekly cron |
| **External pen test** | Manual annual audit | Yearly (~$8–16k) — see [SECURITY.md](SECURITY.md) |

### 9. Synthetic monitoring — prod

Continuous Playwright-based "is the dashboard up?" scripts running from external regions via UptimeRobot / BetterStack. Hits `app.ebay-soft.com`, performs login + dashboard load with a dedicated synthetic-user tenant. Alerts if median round-trip > 5s or any 5xx.

---

## Per-service test setup

Each Java service ships:

```
services/<name>/
├── src/test/java/com/ebaysoft/<service>/
│   ├── unit/                  # pure JUnit 5
│   ├── slice/                 # @WebMvcTest, @DataR2dbcTest
│   ├── integration/           # @SpringBootTest + Testcontainers
│   ├── contract/              # Pact provider verification
│   └── BaseIntegrationTest.java
├── src/test/resources/
│   ├── application-test.yml
│   ├── wiremock/              # external API stubs
│   ├── flyway-test/           # extra migrations only used in tests (rare)
│   └── fixtures/              # JSON fixtures, sample CSVs, etc.
└── build.gradle.kts           # depends on libs/common-test
```

`libs/common-test/` provides:

- `BaseIntegrationTest` — Testcontainers spin-up with `@ServiceConnection`
- `WireMockEbay`, `WireMockStripe`, `WireMockTwilio` — pre-configured external API stubs
- `TestTenantFactory` — creates a tenant + linked eBay account in 3 lines
- `MoneyAssertions` — fluent `assertThat(money).isEqualToInUSD("12.34")`
- Pact JVM helpers wired to the local Broker

`ml-api` (Python) mirrors this with pytest + pytest-fixtures + Testcontainers-Python.

The SPA (React) ships its own `tests/` tree:
- `tests/unit/` — Vitest + React Testing Library
- `tests/integration/` — Vitest with MSW (Mock Service Worker) for API mocking
- `tests/e2e/` — Playwright

---

## Test environments

The big question: do we need a permanent 4th environment (test) in addition to dev / stg / prod? **No.**

| Test layer | Where it runs | Why |
|---|---|---|
| Unit + slice | CI runner (GitHub Actions) | Pure JVM, no infra |
| Integration | CI runner | Testcontainers spin up Postgres / Redis / MinIO inside the runner |
| Contract (Pact) | CI runner + Pact Broker (Hetzner) | Lightweight, contracts are JSON |
| API E2E | **Ephemeral env** spun up by CI from `compose.test.yml` | Full stack, ~5 min to come up, torn down after run |
| UI E2E | Same ephemeral env | Playwright targets the running ephemeral stack |
| Load (Gatling) | Dedicated load-test target — initially **stg outside business hours** | Real-ish data shape, no risk to prod |
| Manual QA / exploratory | **stg** (`staging.ebay-soft.com`, eBay Sandbox) | The "where humans poke things" env |
| Pre-prod smoke | **stg → prod** after each deploy | 30s of automated post-deploy checks |
| Synthetic monitoring | **prod** | Continuous, externally hosted |

### Ephemeral E2E environment

Spec:

```yaml
# compose.test.yml — used only by CI's e2e job
services:
  postgres:
    image: postgres:17
    tmpfs: /var/lib/postgresql/data    # in-memory; fast, no persistence needed

  redis:
    image: redis:7-alpine

  minio:
    image: minio/minio
    command: server /data
    tmpfs: /data

  wiremock-ebay:
    image: wiremock/wiremock:3
    volumes: [./test-fixtures/ebay:/home/wiremock/mappings]

  wiremock-stripe:
    image: wiremock/wiremock:3
    volumes: [./test-fixtures/stripe:/home/wiremock/mappings]

  wiremock-twilio:
    image: wiremock/wiremock:3
    volumes: [./test-fixtures/twilio:/home/wiremock/mappings]

  mailhog:
    image: mailhog/mailhog:latest     # captures outbound email for assertion

  # ... all 12 application services with EBAY_BASE_URL=http://wiremock-ebay:8080, etc.
```

A single GitHub Actions job:

1. `docker compose -f compose.test.yml up -d` (~90s for cold start, ~30s warm).
2. Wait for `/actuator/health` on every service.
3. Run REST-assured API E2E suite (~2–3 min).
4. Run Playwright UI E2E suite against `localhost:443` (~3–4 min).
5. Always run `docker compose down -v` at the end (cleanup).

Total: 8–10 min per release-candidate run. Acceptable because it only runs on release tags and on `main` after every merge, not on every PR.

### Why we don't need a permanent test env

- **State pollution** — a long-lived test env accumulates leftover data, broken records from prior failed tests, half-migrated schemas. Ephemeral starts clean every time.
- **Cost** — 12 services + DB + Redis + MinIO running 24/7 would eat ~24 GB RAM on the box; ephemeral uses it for 10 min per release and releases it.
- **Parallelism** — multiple release candidates can run in parallel without colliding.
- **Drift** — a permanent env always drifts from prod config; ephemeral is built from the exact compose file used for staging.

The exception: **load testing** initially targets stg (after-hours) because spinning up an ephemeral env with prod-shape data is expensive. Once we have load-test data sets prepared, we can shift to an on-demand large ephemeral env.

---

## Test data management

- **Factories, not fixtures.** Each service has a `TestDataFactory` that builds in-test data via the production code paths (so changes to validation break the test, not the prod). Avoid JSON fixture files for anything except external-API response stubs.
- **No real customer data in tests, ever.** Even anonymized. If a bug is reproducible only with real data, write a regression test that generates the *shape* of the data, not a snapshot of it.
- **Money math test datasets** are large JSON files of orders+fees with hand-computed expected P&Ls. Generated by a one-off script, reviewed by a human, then frozen. Lives in `services/accounting-api/src/test/resources/fixtures/pnl-reconciliation/`.
- **eBay WireMock recordings** refreshed quarterly via `scripts/record-fixtures.sh` against eBay Sandbox. Sanitized (no real buyer PII even in sandbox).

---

## CI gates — what blocks merge vs deploy

**Blocks PR merge:**

- Unit + slice + integration tests all green
- Coverage ≥ 80% (per service, JaCoCo)
- All Pact contracts published, no breaking change without a new version
- ArchUnit rules pass (`no double for money`, `no cross-schema joins`, `no naked BigDecimal#divide`, see [BACKEND.md](BACKEND.md))
- OpenAPI spec matches running app (`openapi-diff` check)
- gitleaks: no secrets
- Renovate / Dependency-Check: no new HIGH/CRITICAL CVEs
- trivy on built image: no HIGH/CRITICAL
- `pact-broker can-i-merge` (says "yes, all consumers/providers are compatible")

**Blocks deploy to stg:** all of the above + API E2E suite on ephemeral env.

**Blocks deploy to prod:** all of the above + UI E2E suite on ephemeral env + `pact-broker can-i-deploy --to-environment prod`.

**Nightly / weekly cron:**

- Mutation tests (Pitest, mutmut, Stryker) — weekly
- Pact-Broker full matrix verification — nightly
- gitleaks full-history scan — weekly
- OWASP ZAP against stg — weekly
- Dependency snapshot — nightly
- DB-restore drill — quarterly (proves backups work, also exercises post-restore integration tests)

---

## Flake policy

Flaky tests are a worse problem than missing tests — they erode trust in the suite.

- **First flake**: re-run once automatically (GitHub Actions retry). If green on retry, log it.
- **Second flake within a week**: the test is **quarantined** — moved to `tests/quarantine/`, runs nightly only, doesn't block PRs.
- **Owner has 5 working days to fix or delete.** No "fix later" — quarantine isn't a permanent home.
- **Weekly flake report**: counts, owners, age. Reviewed in the eng standup.

Flake is almost always caused by: hidden time dependency (use `Clock`), real network access (mock it), shared mutable state (don't), test ordering (don't depend on it). Fix the root cause; don't add `Thread.sleep`.

---

## What we deliberately don't do

- **Cypress.** Playwright is strictly more capable and faster. No reason to add a second E2E framework.
- **Cucumber / Gherkin.** Plain JUnit + `@DisplayName` reads fine without the BDD ceremony layer. Cucumber is right for shops where non-engineers write tests; we don't have that.
- **Hand-written API mocks.** WireMock recordings stay the source of truth. Hand-written mocks drift from reality.
- **A separate "QA team."** Engineers write tests for code they wrote. There is no test-quality function staffed separately. (We can revisit at >€500k MRR.)
- **Test plans in Confluence / Jira.** The test file is the test plan. If a scenario isn't in code, it isn't tested.
- **Snapshot testing for charts or PDFs.** Visual snapshots are flake factories. Test the data layer; manual eyeballing for the visual layer.
- **Chaos engineering at MVP.** Toxiproxy/Chaos Monkey arrive after we have 100 paying customers and an SRE practice.
- **Permanent test env in production-shape data.** Ephemeral covers it; the cost-benefit doesn't justify the burn.

---

## Cross-references

- Money-math test discipline → [BACKEND.md → Money handling](BACKEND.md#money-handling)
- ArchUnit rules enforced in CI → [BACKEND.md](BACKEND.md)
- Secrets in tests (never real, always sealed example values) → [SECURITY.md → Secrets](SECURITY.md#secrets)
- The dev / stg / prod environment split → [ENVIRONMENTS.md → Environments](ENVIRONMENTS.md#environments)
- CI/CD pipeline shapes that invoke these tests → [ENVIRONMENTS.md → CI/CD](ENVIRONMENTS.md#cicd)
- Contract tests gating independent deploys → [ARCHITECTURE.md](ARCHITECTURE.md)
- Pen-test cadence → [SECURITY.md](SECURITY.md)
