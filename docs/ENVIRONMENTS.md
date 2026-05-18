# Environments, CI/CD, version control, project board

Operational setup that survives going from solo founder to a small team without rework.

---

## Version control

**Git on GitHub**, private monorepo `ebay-soft/ebay-soft`. Public repos (SDKs, free tools, docs) under the same `ebay-soft/` org.

Why GitHub over GitLab / Codeberg / self-hosted:

- The hiring funnel is on GitHub. Engineers expect to see your repos there.
- Actions, Sponsors, Projects, CodeQL, Dependabot, Security Advisories — all integrated and free for the scale we'll be at.
- One vendor to manage.

**Disaster-recovery mirror.** Nightly push-mirror of `ebay-soft/ebay-soft` to a self-hosted Forgejo on the Hetzner box (or Codeberg). One GitHub Actions cron job, ~20 lines. If GitHub disappears or suspends the account, we keep the code.

### Branching model

Trunk-based, not Git Flow. Solo or small-team friendly, fewer merge headaches.

- `main` — always deployable. Protected branch: PR-only, 1 review (self-review counts at solo stage), green CI, signed commits, linear history.
- Feature branches off `main`, short-lived (<3 days). Merge via **squash** to keep `main` linear.
- Tags `v1.2.3` per release; each tag triggers production deployment.
- No long-lived `dev` / `develop` / `release/*` branches.

### Conventional Commits

Every commit message follows [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(accounting): add waterfall chart endpoint
fix(sync): handle eBay 429 Retry-After header correctly
chore(deps): bump spring-boot to 3.5.3
docs(architecture): clarify cross-schema rule
```

Drives **automatic semantic versioning** and **changelog generation** via `release-please` GitHub Action. The bot opens a "release PR" that bumps versions and writes `CHANGELOG.md`; merging it tags and releases.

### Commit signing

`git config commit.gpgsign true` everywhere. Use SSH signing (GitHub supports it since 2022) — simpler than GPG. Unsigned commits are rejected by branch protection.

---

## Project board

**GitHub Projects v2** — the new (post-2022) Projects, not the old "Project Classic." Free, integrated with Issues and PRs, supports kanban / table / roadmap views.

Why not Linear / Jira / Trello at our scale:

- **Linear** (free up to 250 issues, 2 teams): nicer UX but we'll hit the issue cap in 6 months; migrating later is painful.
- **Jira free** (10 users): heavyweight, slow, configuration overhead. Right tool when you have a project-manager headcount; wrong when you don't.
- **Trello free**: no integration with code or PRs. Throwaway-grade.
- **Plane.so** (open-source, self-hosted): credible Linear alternative if we ever need to leave GitHub Projects. Worth knowing about.

### Board structure

One project per major work surface, not one per repo:

| Project name        | Purpose                                            | Views                                  |
|---------------------|----------------------------------------------------|----------------------------------------|
| **Roadmap**         | Multi-quarter feature work, mapped to [ROADMAP.md](ROADMAP.md) | Roadmap by Quarter, table by Service, kanban by Status |
| **Sprint**          | Current 2-week iteration                           | Kanban (Backlog → In Progress → Review → Done) |
| **Bugs & Support**  | Customer-reported issues; never starved            | Sorted by severity, oldest-first       |
| **Security**        | Private project; CVEs, threat-model items          | Restricted view                        |

Fields on every issue: `service` (one of the 12), `severity`, `iteration`, `effort` (XS/S/M/L), `customer-reported` (boolean).

Automations (built-in, no Action needed):

- New issue → added to Sprint, status `Backlog`.
- PR opened referencing issue → status flips to `In Review`.
- PR merged → status `Done`.

### Issue templates

In `.github/ISSUE_TEMPLATE/`:

- `bug-report.yml` — reproduction, expected vs actual, affected service dropdown.
- `feature-request.yml` — user story, success criteria, target service.
- `support-request.yml` — for customer-facing items routed in by support staff later.

### Roadmap discipline

The Roadmap project mirrors `docs/ROADMAP.md`. If they drift, the doc wins for strategy and the project wins for what's actually scheduled. Once a quarter, reconcile both.

---

## CI/CD

**GitHub Actions** — best fit because we're already on GitHub.

- 2,000 free private-repo minutes/month on the Free plan, 3,000 on Team.
- For more capacity: **self-hosted runner on the Hetzner box** (the AX102 has spare cores). Unlimited minutes, ~zero marginal cost, but: never run untrusted-PR workflows on self-hosted runners. Configure self-hosted runners for `main` builds and deploys only; public-PR builds run on GitHub-hosted free minutes.

### Pipelines

```
on:  push to main          on:  pull_request               on:  tag v*.*.*
└─ ci-main.yml             └─ ci-pr.yml                    └─ release.yml
   ├─ matrix per service      ├─ matrix per changed service   ├─ build all images, tag with version
   ├─ build + test            ├─ build + test                 ├─ push to GHCR
   ├─ contract tests          ├─ contract tests               ├─ run db-migration smoke on stg
   ├─ trivy scan              ├─ trivy scan                   ├─ deploy to stg
   ├─ SBOM (cyclonedx)        ├─ openapi diff vs main         ├─ smoke tests on stg
   ├─ push image to GHCR      └─ comment summary on PR        ├─ wait for manual approval gate
   └─ deploy to stg                                           ├─ deploy to prod
                                                              └─ post-deploy smoke + alert
```

Matrix-by-service means only services whose sources changed are rebuilt. Path filter:

```yaml
jobs:
  detect-changes:
    outputs:
      services: ${{ steps.filter.outputs.changes }}
    steps:
      - uses: dorny/paths-filter@v3
        id: filter
        with:
          filters: |
            auth-api:        ['services/auth-api/**', 'libs/**']
            ebay-conn-api:   ['services/ebay-conn-api/**', 'libs/**']
            # ... one entry per service
```

A change in `libs/common-*` triggers a rebuild of every service, as it should.

### Required CI checks (block merge)

- Per-service: build, unit tests, integration tests (Testcontainers), `openapi.yaml` matches running app, ArchUnit (no `double`/`float` for money, no naked `BigDecimal#divide`).
- Cross-service: consumer-driven **Pact** contract tests against a self-hosted Pact Broker. If `accounting-api`'s OpenAPI changes in a breaking way, `analytics-api`'s CI fails. Full strategy in [TESTING.md](TESTING.md).
- Repo-wide: secret scanner (Gitleaks), dependency check (OWASP / Dependabot), license check (no GPL/AGPL transitives in BSL repo), trivy on built images (fail on HIGH/CRITICAL).

### Releases

`release-please` reads conventional commits, calculates new version, opens a release PR per service that has unreleased changes. Each service is versioned independently — `auth-api v1.4.2`, `sync-api v0.9.1`, etc. When the release PR is merged, the `release.yml` workflow:

1. Builds the production Docker image tagged with the new version + commit SHA.
2. Pushes to GHCR.
3. Updates the `SERVICE_SHAS.env` file in the deploy repo via PR.
4. Once merged, an Ansible playbook deploys the changed service to staging.
5. Smoke tests run on staging.
6. Manual approval gate.
7. Same playbook deploys to production.
8. Post-deploy smoke + Sentry/Grafana alert window.

Time from green CI to production: **<10 minutes** at MVP, **<8 minutes** by M12.

### Self-hosted runner setup

On the Hetzner box, one runner per environment label (`stg`, `prod`). Runners run inside their own Docker container with no access to the host or other services. Tokens auto-rotate weekly via a tiny cron.

Never run `pull_request` from forks on self-hosted runners — that's a remote-code-execution invitation. GitHub-hosted runners handle PRs.

---

## Environments

Three: `dev`, `stg`, `prod`. Plus a transient `preview` per PR if/when we can afford it.

| Env       | Where                                              | DNS                                    | eBay keyset | Data                                       | Who has access      |
|-----------|----------------------------------------------------|----------------------------------------|-------------|--------------------------------------------|---------------------|
| **dev**   | Each developer's laptop, Docker Compose            | `localhost.ebay-soft.dev` (mkcert TLS) | Sandbox     | Synthetic + Wiremock recordings            | The developer       |
| **stg**   | Same Hetzner box as prod, separate compose stack, different ports + bind addr | `staging.ebay-soft.com` (basic-auth in front) | Sandbox | Anonymized subset of prod, refreshed weekly | Team + invited testers |
| **prod**  | Hetzner AX102 main stack                            | `app.ebay-soft.com`, `api.ebay-soft.com` | Production  | Real customer data                         | Customers + ops     |
| preview   | (optional, later) per-PR ephemeral compose         | `pr-123.preview.ebay-soft.com`         | Sandbox     | Anonymized subset                          | PR author + reviewer|

### Hard rules

1. **Production data flows only downward, and only anonymized.** A nightly job `pg_dump`s prod, runs an anonymizer (`safedata` or a homegrown SQL script: emails → `user-<uuid>@example.invalid`, addresses redacted to city/country, eBay tokens dropped entirely), restores to stg.
2. **stg uses the eBay Sandbox keyset, not Production.** Same OAuth client app on eBay's Sandbox tier. Stg can't accidentally write to a real seller's listings.
3. **Each env has its own secrets file** (`secrets.dev.enc`, `secrets.stg.enc`, `secrets.prod.enc`), encrypted with different `age` keys. The prod `age` private key never leaves the founder's machine + the Hetzner box.
4. **Each env has its own Stripe account / mode.** Stripe test mode for dev + stg, live mode for prod. No exceptions — a misrouted webhook in prod from a test event would cause real refunds.
5. **Domain → env mapping** is enforced at the gateway. The same Docker image with `SPRING_PROFILES_ACTIVE=stg` running on the staging port refuses any request whose `Host` header isn't `staging.ebay-soft.com`. Belt-and-suspenders.

### Local dev

Each developer runs:

```bash
just up                  # docker compose up -d --build everything
just seed                # synthetic tenant + eBay sandbox account + 200 orders
just logs sync-api       # tail one service's logs
just test inventory-api  # run that service's tests
just stop
```

(`just` from [casey/just](https://github.com/casey/just) — `Makefile` replacement that nobody hates.)

Total RAM needed on a dev laptop with all 11 services + Postgres + Redis: ~10–12 GB. A 16 GB laptop is the practical minimum.

### Configuration

`application.yml` ships with safe defaults; `application-{dev,stg,prod}.yml` overrides per env. Spring profile activated via `SPRING_PROFILES_ACTIVE` env var passed by `docker compose`. Secrets are env vars sourced from the env-specific encrypted file.

### Domain & DNS layout

| Subdomain                  | Purpose                                                       |
|----------------------------|---------------------------------------------------------------|
| `ebay-soft.com`            | Marketing site (static / Astro)                               |
| `app.ebay-soft.com`        | Production SPA                                                |
| `api.ebay-soft.com`        | Production API gateway                                        |
| `staging.ebay-soft.com`    | Staging SPA + API (basic auth or Cloudflare Access)           |
| `docs.ebay-soft.com`       | Public developer / API docs                                   |
| `status.ebay-soft.com`     | Statuspage (Statuspage.io, Hund, or self-hosted Cachet)       |
| `*.preview.ebay-soft.com`  | Per-PR previews (optional, later)                              |

Cloudflare DNS, proxy enabled on the public ones, TLS via Cloudflare or Let's Encrypt at the origin (or both — pass-through).

### Promotion flow

Code path: `dev (local)` → PR → CI green → merge to `main` → auto-deploy `stg` → smoke + manual approval → tag release → auto-deploy `prod`.

Config & secret path: changes via PR to the encrypted config files, same gates.

DB migration path: Flyway, baked into each service's startup. Migrations must be **expand → migrate → contract** so each release N is compatible with release N-1 still running for the few seconds of rollout.

---

## Observability across environments

- One Grafana instance per env (`grafana.ebay-soft.com`, `grafana.staging.ebay-soft.com`), behind Cloudflare Access.
- Loki and Tempo write to separate stores per env. Never cross-query.
- Sentry projects per env (`ebay-soft-prod`, `ebay-soft-stg`); env tag on every event.
- UptimeRobot / BetterStack pings prod from at least 3 regions.

---

## What this section deliberately doesn't include

- Kubernetes manifests — we're on Docker Compose until ~2k paying customers.
- Multi-region or active-passive failover — one Falkenstein DC suffices through year 2.
- Canary deployments — per-service rolling restart on a single box is enough; 10–15s of 503 is acceptable behind the gateway's friendly retry UI.
- Feature flags — start with config toggles, add a real flag system (Unleash self-hosted) only when a feature actually needs gradual rollout.
