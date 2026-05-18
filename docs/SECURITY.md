# Security

This file is the **technical** side of protecting user data: threat model, encryption, RLS, secrets, incident response. The **user-facing** side — what we tell sellers in the Privacy Policy, sub-processor list, DPA, cookies notice, and the lawful-basis-per-data-type table — lives in [LEGAL.md](LEGAL.md). The two are intended to be read together: every commitment in LEGAL.md is implemented by a control in this file.

## Threat model (short version)

Our crown jewels, in priority order:

1. **eBay refresh tokens** — these let an attacker act as the seller, including changing prices and reading buyer addresses. Worst case scenario for ebay-soft.
2. **Seller financial data** — fees, payouts, P&L. Sensitive, regulated under GDPR.
3. **Buyer PII** — names, addresses, emails of the seller's customers. Regulated under GDPR. We minimize what we store.
4. **Seller credentials** — email, password hash. Bog standard.
5. **Our internal admin & deploy keys** — operational risk.

Plausible attackers:

- Credential-stuffing bots aiming at the login form.
- A competitor or activist trying to scrape our public marketing pages or break in to leak data.
- A malicious user inside an organization (multi-user tier).
- A compromised dependency / supply chain attack.
- A misconfigured deploy leaking secrets to logs or backups.

## Defenses

### Authentication

#### Password storage

- **Hash:** Argon2id, parameters `m=65536` (64 MiB), `t=3`, `p=2`. Bcrypt with `cost=12+` is an acceptable fallback if a Java native-image build ever blocks Argon2.
- **Salt:** automatic. Argon2id (and bcrypt) generate a unique random salt per password and store it inside the encoded hash. We never write salt-handling code — Spring Security's `Argon2PasswordEncoder` does it. A doc, tutorial, or PR that hand-rolls salt is wrong by definition; reject it.
- **Pepper:** mandatory. An app-wide secret HMAC'd into the password before Argon2 hashing. If only the DB leaks (the common breach pattern), every hash becomes effectively unbreakable. Implementation:

  ```
  stored_hash = Argon2id( HMAC_SHA256( pepper_vN, user_password ) )
  ```

  The pepper is loaded by `auth-api` from the env var `AUTH_PASSWORD_PEPPER` (sourced from the sealed file — see [Secrets](#secrets)). It is never in the database, never in logs, never in error messages, never in stack traces. A unit test verifies the masking converter redacts it.

- **Versioned peppers** — every stored hash row carries `pepper_version SMALLINT NOT NULL`. The active version is set by env var `AUTH_PASSWORD_PEPPER_VERSION`. Past peppers stay loadable (via `AUTH_PASSWORD_PEPPER_V1`, `AUTH_PASSWORD_PEPPER_V2`, ...) so existing users can still log in during rollover. On a successful login at a stale version, we **rehash with the current pepper version and Argon2 parameters** and update the row. After a defined deadline (typically 12 months after rotation), unrotated users are force-expired on next login.

- **Pepper rotation cadence** — yearly by default, immediately on any suspicion of `auth-api` server compromise. Rotation = generate a new random 32-byte key, add it as `AUTH_PASSWORD_PEPPER_V{n+1}`, flip `AUTH_PASSWORD_PEPPER_VERSION` to `n+1`. No code change. Old keys retained until natural migration or force-expiry.

#### Password rules for users

- **Minimum length: 12 characters.** No required-symbol rules, no periodic forced rotation, no "must contain a digit" theater (those rules drive `Password1!` → `Password2!`, which is worse than nothing — NIST SP 800-63B has been explicit on this since 2017 and reaffirmed in 2024).
- **HIBP breach check** at sign-up and password change. Use the [k-anonymity API](https://haveibeenpwned.com/API/v3#PwnedPasswords): client SHA-1's the password locally, sends only the first 5 hex chars, receives the list of hash suffixes, checks locally. We never transmit the password. Pwned passwords are **rejected**, not warned about.
- **Client-side strength meter** with `zxcvbn` (or `@zxcvbn-ts/core`) — guides, doesn't gatekeep. Combined with the HIBP check, this catches the long tail without annoying competent users.
- **Re-hash on successful login when parameters changed** — lets us raise Argon2 cost or rotate pepper version organically over weeks of normal usage.
- Login form runs over TLS 1.3, posts to a Bucket4j-rate-limited endpoint, returns identical responses for "wrong password" and "user doesn't exist" (prevents user enumeration).

#### Multi-factor & session

We support three second factors, in declining order of security:

| Factor                  | Strength                             | Cost per use   | When to use                                                    |
|-------------------------|--------------------------------------|----------------|----------------------------------------------------------------|
| **Passkey (WebAuthn)**  | Strongest — phishing-resistant       | $0              | Default for new sign-ups (2026+). Strongly encouraged for all. |
| **TOTP** (authenticator app) | Strong — needs device theft + PIN | $0              | Recommended for all users; required on Pro and Scale tiers.    |
| **SMS OTP**             | **Weakest** — vulnerable to SIM-swap and SS7 attacks; NIST SP 800-63B has flagged SMS as "Restricted" since 2017 | ~$0.04–$0.08 per SMS via Twilio (varies by country) | **Fallback only**, for users who refuse or can't install an authenticator. Never the sole factor for high-value actions. |

Rules:

- Every account must enrol at least one of passkey / TOTP **before** SMS OTP can be enabled. SMS is never the only second factor.
- **High-value actions require passkey or TOTP**, never SMS: disconnecting an eBay account, changing billing payment method, viewing API keys, account closure. The UI grays out these actions for SMS-only users with an explanation.
- The user can have *multiple* factors enrolled (e.g. passkey + TOTP + SMS as backup). At login we let them choose; for step-up we pick the strongest available.

**SMS provider: Twilio Verify**

We use the [Twilio Verify API](https://www.twilio.com/docs/verify), not raw Programmable Messaging. Verify is the right choice for OTP because:

- Twilio generates and stores the OTP server-side; we don't roll our own random + hash + TTL. They handle replay, rate-limit, and brute-force defenses.
- Built-in fraud signals (carrier risk score, SIM-swap velocity, line-type filtering).
- Cheaper than raw SMS for the OTP use case — Verify is billed per verification attempt, not per SMS, with retries included.
- Supports adding voice-call, WhatsApp, and email channels later by changing one parameter — no second integration.

Implementation:

- The Twilio call lives in **`auth-api`** (not `notif-api`). OTPs are on the auth-critical path and synchronous; routing them through the notification service would add latency and a failure point for no benefit. `notif-api` stays for non-auth transactional notifications.
- `auth-api` calls Twilio via the official Java SDK using a `WebClient` wrapper that adds: timeout (3 s), retry (idempotent, with `Idempotency-Key` set to a server-side request id), circuit breaker, and per-tenant rate limit (3 SMS / 15 min / phone number).
- We **never** store the OTP code or its hash — Twilio holds it. We store only the Twilio `verification_sid` (a reference) with a short TTL.

**Phone-number storage & verification**

- Phone numbers are stored on `users.phone_e164` in **E.164 format** (`+48555123456`), validated client-side and server-side with `libphonenumber`.
- A new or changed phone number is **not trusted** until a one-time verification succeeds. `phone_verified_at` is set only after Verify reports success.
- Phone numbers are PII under GDPR — covered in [LEGAL.md](LEGAL.md) sub-processor and lawful-basis tables, retained for the lifetime of the account + 30 days.
- Phone is **never logged** — the masking converter redacts everything that matches an E.164 pattern.

**Anti-fraud**

- Rate limit: max 3 OTP sends per phone number per 15 minutes, max 10 per day. Enforced via Bucket4j + Redis before we call Twilio (also enforced by Twilio, but we prefer to spend $0 hitting our own limiter than $0.05 hitting theirs).
- Country gate: by default, we send to the seller's country and the major eBay marketplaces. Sending to high-risk countries (Twilio publishes the list) requires the user to be on a paid plan + flagged on the account.
- Verify webhook on **carrier-reported SIM swap** in the last 7 days → block SMS step-up on that number, require TOTP/passkey re-enrolment.
- Failed-verification counter on the row: 5 failed codes → 30-minute cool-off, audit-log entry, in-app warning banner.

**Session & lockout (unchanged from before)**

- **Session:** short-lived JWT (15 min) + httpOnly secure SameSite=strict refresh cookie (7 days, rotating on every use). Refresh-cookie reuse after rotation = treat as theft; invalidate the family and force re-login.
- **Account lockout:** exponential backoff per identifier+IP, not a hard lock. After 5 failed attempts: 1s, 5s, 30s, 5m, 1h. A hard lock is a denial-of-service waiting to be weaponized.
- **Login from new device** → email confirmation link before the session activates.
- **Login from new country** → email + step-up to the strongest enrolled factor (passkey or TOTP; SMS is not acceptable for new-country step-up).
- **Successful auth events** (login, password change, 2FA enrolment, phone change) generate an in-app + email notification so the user sees real-time activity on their account.

### eBay OAuth tokens (the crown jewels)

- Refresh tokens stored in `ebay_accounts.refresh_token_enc` as **AES-256-GCM**.
- Encryption key (DEK) wrapped by a master key (KEK) loaded from env at boot. KEK is **never** written to disk in plaintext.
- Access tokens cached in Redis with TTL matching token lifetime; not persisted.
- Every read of a refresh token is audited.
- Tokens never appear in logs — we have a Logback `MaskingConverter` and a unit test that fails the build if the pattern appears.

### Transport

- TLS 1.3 everywhere. TLS 1.2 only as fallback for legacy clients (logged & alerted).
- HSTS with `preload`.
- Strict CSP (`default-src 'self'; script-src 'self' 'wasm-unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https://i.ebayimg.com; connect-src 'self' https://api.ebay-soft.com`).
- `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy` restrictive.

### Application

- **Spring Security** with method-level `@PreAuthorize`.
- Tenancy enforced two ways: application-layer `tenant_id` filter + Postgres Row-Level Security on sensitive tables (see [DATABASE.md](DATABASE.md)).
- Input validation on every DTO (`jakarta.validation`).
- Output encoding on every Thymeleaf template (we mostly don't use templates, but PDF reports do).
- CSRF protection on all state-changing endpoints (token-cookie pattern for the SPA).
- Rate limiting on auth endpoints (Bucket4j or Redis-backed).
- File upload limited to allowed mime-types, size-capped, virus-scanned (ClamAV).

### Dependency hygiene

- **Dependabot / Renovate** for both Java and JS.
- **OWASP Dependency-Check** in CI; build fails on `CVSS ≥ 7.0`.
- **SBOM** generated per build (CycloneDX), retained 1 year.
- **trivy** scan on every container image; CI fails on HIGH or CRITICAL.

### Runtime

- App runs as a **non-root** user inside the container.
- Container filesystem read-only except `/tmp`.
- `seccomp` default profile.
- Drop all Linux capabilities, add only what's needed.
- Resource limits set per container (no fork-bomb DoS).

### Secrets

The main repo is **public** (BSL — see [GOVERNANCE.md](GOVERNANCE.md)). Treat every file as if it will appear on a competitor's screen tomorrow. **No plaintext secret ever enters git.** This is non-negotiable; one leaked production key undoes years of trust.

#### What counts as a secret

Every one of these is a secret. If you see it in a PR diff, stop and rotate:

- Database passwords, connection strings with embedded creds
- Redis passwords, MinIO root keys
- JWT signing keys (private side), JWKS private keys
- KEK / DEK encryption keys (for eBay refresh-token encryption at rest)
- eBay app `client_secret`, OAuth client secrets for any third party
- Stripe live keys (`sk_live_*`), webhook signing secrets
- **Twilio Account SID, Auth Token, Verify Service SID** — authenticate the SMS-OTP service
- Postmark / Resend server tokens
- Cloudflare API tokens
- SMTP credentials
- LLM provider API keys
- OTel collector tokens, Sentry DSNs *with auth*, Grafana Cloud tokens
- SSH private keys, deploy keys, `age` private keys
- GitHub PATs, GHCR pull tokens
- Internal service-account JWT seeds
- Recovery codes, 2FA setup secrets (per user — never logged either)

If unsure, treat it as a secret and ask. Cheap to be paranoid; expensive to be sorry.

#### Where secrets live at runtime — three legitimate sources

1. **Environment variables** — set by `docker compose` from a sealed file on the server, OR set by GitHub Actions Secrets for CI jobs. This is the **default**.
2. **A file on the server** mounted into the container at a fixed path (`/run/secrets/<name>`) — used for multi-line secrets like PEM private keys.
3. **A secret manager** — Hashicorp Vault or Infisical, self-hosted. Adopted when the team grows past 2 people; not at MVP.

Application code reads secrets only via Spring's `@Value("${env.var.name}")` or `@ConfigurationProperties`. No application code reads files directly except the `common-security` lib that loads JWT/KEK PEMs from `/run/secrets/`.

#### The "sealed file" pattern — how we get from git to server

Production secrets are stored encrypted **inside the public repo** at `infra/secrets/{dev,stg,prod}.enc.yml`. The encryption is **sops** with **age** recipients:

```bash
# encrypt (developer, after editing)
sops --encrypt --age $(cat .age/team.pub) infra/secrets/prod.yml > infra/secrets/prod.enc.yml

# decrypt on the server at deploy time, via Ansible
sops --decrypt --age-key-file /root/.age/server.key infra/secrets/prod.enc.yml \
  | docker compose --env-file /dev/stdin up -d
```

- Only the **encrypted file** (`.enc.yml`) is committed. The plaintext `.yml` is `.gitignore`'d.
- The age **private** key lives in two places only: the founder's laptop (in a password manager), and the production server (`/root/.age/server.key`, mode 0600, owned by root). It is **never** in the repo.
- New developers get added by editing `.sops.yaml` to include their age public key, then re-encrypting. Removing a developer means rotating every encrypted file plus the secrets themselves.

This pattern is auditable (you can see which files were touched in PRs), versioned (`git log infra/secrets/prod.enc.yml`), and zero-trust on the repo host — GitHub seeing the encrypted bytes leaks nothing.

#### Local development

- `.env` files are **never committed**. Top-level `.gitignore` includes `.env`, `.env.*`, `*.pem`, `*.key`, `.age/`, `secrets/`.
- Every service ships an **`.env.example`** in source control with the variable names + safe placeholder values. Developers copy it to `.env` and fill in their own dev creds (Stripe test keys, eBay Sandbox keys).
- **No production secret ever leaves the server.** Even for debugging. If a developer needs to reproduce a prod issue, they get a Sandbox-equivalent setup, not the real keys.

#### CI/CD secrets

- Stored in **GitHub Actions Secrets** at the org and per-environment level (`prod` env gets a manual approval gate before its secrets are accessible — see [ENVIRONMENTS.md](ENVIRONMENTS.md)).
- A workflow can reference `${{ secrets.STRIPE_LIVE_KEY }}`, but the value is masked in logs and never echoed.
- **Workflow files that run on `pull_request` from forks are denied access to secrets** (GitHub enforces this by default; never override it).
- Self-hosted runners never see prod secrets — they're scoped to `main`-branch builds and deploys, with secrets injected only at the deploy step.

#### Detection: prevent the accidental commit

Three layers:

1. **Pre-commit hook** (every developer's machine): `gitleaks protect --staged --no-banner`. Catches obvious patterns (Stripe keys, AWS, eBay, JWT) before the commit happens. Installation is one line in `CONTRIBUTING.md`; refusal to install it is grounds to revoke commit access.
2. **CI scan** (every PR): `gitleaks detect --redact --log-opts="-1"` over the diff. Failing finding blocks merge. Full-history scan runs weekly as a separate workflow.
3. **GitHub Push Protection** (free feature on public repos): GitHub itself scans pushes for known-format secrets and rejects them. Enable it on the org.

#### What to do when a secret leaks

A leak = commit pushed + secret is in plaintext. Treat as a confirmed incident immediately:

1. **Rotate the secret** at its source (Stripe dashboard, eBay dev console, etc.) within **15 minutes** of discovery. Do not wait to "investigate first."
2. **Revoke** the old credential explicitly — many providers leave both old and new active during rotation; that's a window of risk.
3. **Audit** what could have been accessed with that credential (Stripe events, eBay API calls, server logins) over the entire window the secret existed in any git history — not just the last commit.
4. **Notify** affected customers per the breach policy in [LEGAL.md](LEGAL.md) if buyer/seller PII could have been read.
5. **Do not** try to "fix" by rewriting git history (`git filter-repo`). The secret is already on GitHub's servers, on every clone, on every fork. Once leaked, always leaked. Rotation is the only real fix.
6. **Post-mortem** within a week: how did the pre-commit hook fail to catch it, why did CI miss it, what change to controls would have prevented this class of leak.

#### Rotation cadence

| Secret class                                  | Rotation                                                |
|-----------------------------------------------|---------------------------------------------------------|
| Stripe live key, webhook secret               | Yearly + on suspicion + on team change                  |
| eBay app `client_secret`                      | On team change + every 2 years                          |
| KEK (master key encrypting eBay tokens)       | Yearly + on suspicion (re-encryption tooling required)  |
| JWT signing key (`auth-api`)                  | Quarterly + on suspicion (publish overlapping JWKS for graceful rollover) |
| Internal service-account JWT seeds            | Quarterly                                               |
| `age` private keys                            | On team change; full re-encryption of all sealed files  |
| SSH host & deploy keys                        | On team change + yearly                                 |
| GitHub Actions Secrets (per-env)              | Yearly; immediately on incident                          |
| Postmark/Resend/Cloudflare/Sentry API tokens  | Yearly                                                  |
| Twilio Auth Token                             | Yearly + on suspicion (Twilio supports overlapping primary/secondary tokens for zero-downtime rotation) |

A simple `secrets-rotation.md` runbook lists each secret, when it was last rotated, and the command to rotate it. Quarterly review.

### Logging — what we DON'T log

- Access tokens, refresh tokens.
- Passwords, password hashes.
- Full credit card numbers (Stripe gives us last4 only — that's fine).
- Buyer addresses (we redact street and number, keep city/country).
- 2FA secrets / recovery codes.

A unit test reads our `MaskingConverter` patterns and a representative log sample; CI fails if any sensitive pattern slips.

### Audit

- Every privileged action (token read, admin login, plan change, eBay account disconnect) writes a row to `audit_log` with actor, target, IP, timestamp.
- Audit log is append-only — there's no `UPDATE` or `DELETE` permission on it for the app role.

## GDPR / data protection

- DPA available to sellers on request and prominently linked in our ToS.
- **Subject access** — sellers can export everything we hold on them via a self-serve endpoint.
- **Right to erasure** — sellers can delete their account; we hard-delete within 30 days. Anonymized analytics OK to retain.
- **Buyer PII** — purged 90 days after order completion unless seller has an active dispute or return.
- **Sub-processors** — Stripe, Cloudflare, Postmark, Hetzner, optional LLM provider — all DPA-covered and listed publicly.
- **DPIA** done for the ML profiling features (Phase 2+).

## eBay-specific compliance

- **Marketplace Account Deletion / Closure** webhook **mandatory** — we listen, verify the challenge, and hard-delete within 24h.
- We follow eBay's API License Agreement: no scraping, no caching beyond TTLs they prescribe, no resale of marketplace data.
- We display API attribution where required.

## Vulnerability disclosure

- Public `security.txt` at `https://ebay-soft.com/.well-known/security.txt` with PGP key and contact.
- Bug bounty (informal, recognition-only at MVP; cash-based later via HackerOne or YesWeHack).
- 90-day disclosure timeline standard.

## Incident response

- Defined severities S1–S4 with response times.
- Runbook for each plausible incident: token leak, DB breach, ransomware, eBay API outage, Stripe webhook drop, deploy gone wrong.
- 72-hour GDPR breach notification clock starts from confirmed PII exposure; we have a pre-drafted notification template.
- Post-incident review (blameless) written within a week, shared internally; the customer-visible version is published if any customer was impacted.

## Security review cadence

- Monthly: dependency audit, SBOM diff, log review for anomalous patterns.
- Quarterly: backup restore drill, threat-model refresh.
- Yearly: external penetration test (~$8–16k). Schedule for month 8 so a tested product is ready for enterprise sales.
