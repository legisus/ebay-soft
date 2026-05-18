# Database — PostgreSQL schema sketch

PostgreSQL 17 is the single source of truth. **One cluster, one schema per service** (see [ARCHITECTURE.md](ARCHITECTURE.md) for the full service list). Each service has its own DB role with access only to its own schema; cross-schema joins are forbidden — services talk to each other over REST or via events.

We use:

- **TimescaleDB** extension (optional, in the `analytics` schema for time-series hypertables) — automatic partitioning without leaving Postgres.
- **pg_partman** as a fallback if TimescaleDB licensing concerns appear.
- **pgvector** in the `sync` schema for embedding-based listing similarity ("find me similar listings to mine and their prices").

Connection topology:

```
WebFlux services (ebay-conn, sync, analytics)  ── R2DBC pool ──┐
                                                                 ├──► PgBouncer (transaction mode) ──► Postgres primary
MVC services (everything else, virtual threads) ── HikariCP  ──┘
                                                                                                        ▼
                                                                                                     Postgres replica (read-only, async streaming)
```

Read replicas are used by `analytics-api` and `admin-api` only.

---

## Money columns

Every monetary value in this database maps to the **`Money`** value type defined in `libs/common-domain` (see [BACKEND.md → Money handling](BACKEND.md#money-handling) for the full type definition and the rules around `BigDecimal`).

`Money` is a `record Money(BigDecimal amount, Currency currency)`. On disk this becomes **two columns** that travel together:

| Domain field          | DB columns                                              |
|-----------------------|---------------------------------------------------------|
| `Money unitPrice`     | `unit_price NUMERIC(12,2) NOT NULL`, `unit_price_currency CHAR(3) NOT NULL` |
| `Money fees`          | `fees NUMERIC(12,2) NOT NULL`, `fees_currency CHAR(3) NOT NULL` |
| ...                   | ...                                                     |

When a row has many money columns that always share a currency (an order line, a P&L row), we **denormalize** the currency to a single `currency` column on the row instead of repeating it per amount — saves space and avoids drift. The DDL below uses this pattern: each table has one `currency CHAR(3)` and the numeric columns are amounts in that currency.

### Column widths

Standard widths used throughout. **Do not invent new ones without a documented reason.**

| Use                                              | DB type           | Java side                |
|--------------------------------------------------|-------------------|--------------------------|
| Per-line amounts (unit_price, fees, tax, ship)   | `NUMERIC(12,2)`   | `Money` — max ≈ $99,999,999,999.99 |
| Period aggregates (daily/monthly revenue, LTV)   | `NUMERIC(14,2)`   | `Money` — headroom for yearly P&L on big sellers |
| Percentages / rates (tax rate, margin %)         | `NUMERIC(7,6)`    | `BigDecimal` — not `Money`; e.g. `0.190000` |
| FX rates                                         | `NUMERIC(18,10)`  | `BigDecimal` — source-system precision |
| Currency code                                    | `CHAR(3)`         | `java.util.Currency` (ISO 4217) |

### Forbidden in DDL

- `REAL` or `DOUBLE PRECISION` for money — **never**. Binary floats lose decimal precision, same problem as Java's `double`.
- `MONEY` (the Postgres type) — locale-dependent, lossy, no currency awareness. Use `NUMERIC` + a separate currency column.
- Numeric columns without `NOT NULL` for amounts that should always exist — use `DEFAULT 0` instead. A null amount in accounting is a bug, not a value.

A migration that introduces a money column without an adjacent currency column is rejected in code review.

## Core schema (illustrative DDL)

```sql
-- Tenancy
CREATE TABLE tenants (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name            TEXT NOT NULL,
  plan            TEXT NOT NULL CHECK (plan IN ('free','starter','pro','scale')),
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE users (
  id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id            UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  email                CITEXT NOT NULL UNIQUE,
  password_hash        TEXT,                  -- Argon2id of HMAC-SHA256(pepper, password). NULL if passkey-only or SSO.
  pepper_version       SMALLINT,              -- which AUTH_PASSWORD_PEPPER_Vn was used; see SECURITY.md → Authentication
  password_changed_at  TIMESTAMPTZ,
  role                 TEXT NOT NULL CHECK (role IN ('owner','member','viewer')),
  totp_secret_enc      BYTEA,                 -- AES-256-GCM encrypted with KEK
  phone_e164           TEXT,                  -- E.164 format (+48555123456); NULL until enrolled. UNIQUE per tenant.
  phone_verified_at    TIMESTAMPTZ,           -- set only after Twilio Verify succeeds
  phone_failed_attempts SMALLINT NOT NULL DEFAULT 0,
  phone_locked_until   TIMESTAMPTZ,           -- cool-off after too many failed codes
  created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK ((password_hash IS NULL) = (pepper_version IS NULL)),  -- both set or both null
  CHECK (phone_e164 IS NULL OR phone_e164 ~ '^\+[1-9][0-9]{6,14}$'),
  UNIQUE (tenant_id, phone_e164)
);

-- Active Twilio Verify references — short-lived, NEVER stores the OTP code itself (Twilio holds it).
CREATE TABLE sms_otp_verifications (
  id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id              UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  channel              TEXT NOT NULL CHECK (channel IN ('sms','call','whatsapp')),
  twilio_verify_sid    TEXT NOT NULL,         -- Twilio's reference; not a secret, but useless without our account
  purpose              TEXT NOT NULL,         -- 'enrol','login_step_up','phone_change'
  requested_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  expires_at           TIMESTAMPTZ NOT NULL,  -- typically requested_at + 10 minutes
  consumed_at          TIMESTAMPTZ
);
CREATE INDEX sms_otp_user_idx ON sms_otp_verifications (user_id, requested_at DESC);

-- eBay account linkage (one tenant -> N eBay accounts)
CREATE TABLE ebay_accounts (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  ebay_user_id        TEXT NOT NULL,           -- eBay's stable ID
  marketplace_id      TEXT NOT NULL,           -- EBAY_US, EBAY_DE, ...
  refresh_token_enc   BYTEA NOT NULL,          -- AES-GCM with KMS key
  access_token_enc    BYTEA,
  access_token_expires_at TIMESTAMPTZ,
  scopes              TEXT[] NOT NULL,
  connected_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  status              TEXT NOT NULL DEFAULT 'active',  -- active|disconnected|expired
  UNIQUE (tenant_id, ebay_user_id, marketplace_id)
);

-- Listings (a row per eBay item per account)
CREATE TABLE listings (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  ebay_account_id     UUID NOT NULL REFERENCES ebay_accounts(id) ON DELETE CASCADE,
  ebay_item_id        TEXT NOT NULL,
  sku                 TEXT,
  title               TEXT NOT NULL,
  category_id         TEXT NOT NULL,
  current_price       NUMERIC(12,2) NOT NULL,    -- Money(current_price, currency)
  currency            CHAR(3) NOT NULL,
  quantity_available  INTEGER NOT NULL,
  condition           TEXT,
  format              TEXT NOT NULL,  -- FIXED_PRICE, AUCTION
  status              TEXT NOT NULL,  -- ACTIVE, ENDED, ...
  embedding           VECTOR(384),    -- title+desc embedding for similarity search
  raw                 JSONB NOT NULL, -- full eBay payload, audit
  fetched_at          TIMESTAMPTZ NOT NULL,
  UNIQUE (ebay_account_id, ebay_item_id)
);
CREATE INDEX listings_sku_idx ON listings (ebay_account_id, sku);

-- Orders
CREATE TABLE orders (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  ebay_account_id     UUID NOT NULL REFERENCES ebay_accounts(id) ON DELETE CASCADE,
  ebay_order_id       TEXT NOT NULL,
  buyer_pseudonym     TEXT NOT NULL,  -- never store buyer PII directly
  placed_at           TIMESTAMPTZ NOT NULL,
  paid_at             TIMESTAMPTZ,
  shipped_at          TIMESTAMPTZ,
  total_amount        NUMERIC(12,2) NOT NULL,    -- Money(total_amount, currency)
  currency            CHAR(3) NOT NULL,
  status              TEXT NOT NULL,
  raw                 JSONB NOT NULL,
  UNIQUE (ebay_account_id, ebay_order_id)
);
CREATE INDEX orders_placed_at_idx ON orders (ebay_account_id, placed_at DESC);

CREATE TABLE order_lines (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id            UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  listing_id          UUID REFERENCES listings(id),
  sku                 TEXT,
  quantity            INTEGER NOT NULL,
  -- All amounts below share the order's currency (joined from orders.currency).
  unit_price          NUMERIC(12,2) NOT NULL,         -- Money
  unit_cost           NUMERIC(12,2),                  -- Money — COGS, set by seller or inferred
  shipping_paid       NUMERIC(12,2) NOT NULL DEFAULT 0, -- Money
  tax                 NUMERIC(12,2) NOT NULL DEFAULT 0  -- Money
);

-- Finances: fees, refunds, payouts from the eBay Finances API
CREATE TABLE finance_events (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  ebay_account_id     UUID NOT NULL REFERENCES ebay_accounts(id) ON DELETE CASCADE,
  ebay_event_id       TEXT NOT NULL,
  event_type          TEXT NOT NULL,           -- SALE, REFUND, FEE, DISPUTE, PAYOUT, ADJUSTMENT
  related_order_id    UUID REFERENCES orders(id),
  amount              NUMERIC(12,2) NOT NULL,   -- Money(amount, currency) — negative for fees/refunds outbound
  fee_type            TEXT,                     -- final_value, store_subscription, promoted_listings, ...
  currency            CHAR(3) NOT NULL,
  occurred_at         TIMESTAMPTZ NOT NULL,
  raw                 JSONB NOT NULL,
  UNIQUE (ebay_account_id, ebay_event_id)
);
CREATE INDEX finance_events_occurred_idx ON finance_events (ebay_account_id, occurred_at DESC);

-- Inventory (independent of listings — SKU master)
CREATE TABLE skus (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  code                TEXT NOT NULL,
  name                TEXT NOT NULL,
  cost_per_unit       NUMERIC(12,2),            -- Money(cost_per_unit, cost_currency)
  cost_currency       CHAR(3),                  -- nullable until COGS is entered
  reorder_point       INTEGER,
  reorder_quantity    INTEGER,
  lead_time_days      INTEGER,
  UNIQUE (tenant_id, code)
);

CREATE TABLE warehouses (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  name                TEXT NOT NULL,
  address             JSONB
);

CREATE TABLE stock_levels (
  sku_id              UUID NOT NULL REFERENCES skus(id) ON DELETE CASCADE,
  warehouse_id        UUID NOT NULL REFERENCES warehouses(id) ON DELETE CASCADE,
  on_hand             INTEGER NOT NULL DEFAULT 0,
  reserved            INTEGER NOT NULL DEFAULT 0,
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (sku_id, warehouse_id)
);

-- Repricer rules
CREATE TABLE repricer_rules (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id           UUID NOT NULL REFERENCES tenants(id),
  name                TEXT NOT NULL,
  scope               JSONB NOT NULL,   -- which listings/categories
  strategy            TEXT NOT NULL,    -- BEAT_LOWEST, MATCH_LOWEST, MARGIN_FLOOR
  params              JSONB NOT NULL,
  enabled             BOOLEAN NOT NULL DEFAULT TRUE,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Time-series: aggregated daily P&L per listing (materialized)
CREATE TABLE pnl_daily (
  tenant_id           UUID NOT NULL,
  ebay_account_id     UUID NOT NULL,
  day                 DATE NOT NULL,
  listing_id          UUID,
  currency            CHAR(3) NOT NULL,          -- all amounts below are Money(.., currency)
  revenue             NUMERIC(14,2) NOT NULL DEFAULT 0, -- Money
  cogs                NUMERIC(14,2) NOT NULL DEFAULT 0, -- Money
  fees                NUMERIC(14,2) NOT NULL DEFAULT 0, -- Money
  refunds             NUMERIC(14,2) NOT NULL DEFAULT 0, -- Money
  shipping_cost       NUMERIC(14,2) NOT NULL DEFAULT 0, -- Money
  promoted_cost       NUMERIC(14,2) NOT NULL DEFAULT 0, -- Money
  net_profit          NUMERIC(14,2) NOT NULL DEFAULT 0, -- Money
  units_sold          INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY (tenant_id, ebay_account_id, day, listing_id)
);

-- Billing
CREATE TABLE subscriptions (
  tenant_id           UUID PRIMARY KEY REFERENCES tenants(id) ON DELETE CASCADE,
  stripe_customer_id  TEXT NOT NULL,
  stripe_sub_id       TEXT,
  plan                TEXT NOT NULL,
  status              TEXT NOT NULL,
  current_period_end  TIMESTAMPTZ
);

-- Audit
CREATE TABLE audit_log (
  id                  BIGSERIAL PRIMARY KEY,
  tenant_id           UUID,
  user_id             UUID,
  action              TEXT NOT NULL,
  target              TEXT,
  metadata            JSONB,
  occurred_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

## Mapping `Money` to columns

The DB stores `(amount, currency)` as two columns; the application code sees a single `Money` field. We never let raw `BigDecimal` for monetary values leak out of the persistence layer.

### JPA (MVC services on Hibernate)

A `Money` field maps to two columns via `@Embedded` + `@AttributeOverrides`:

```java
@Embeddable
public record MoneyEmbeddable(
        @Column(name = "amount",   nullable = false, precision = 12, scale = 2) BigDecimal amount,
        @Column(name = "currency", nullable = false, length = 3)                String currencyCode
) {
    public Money toDomain() {
        return new Money(amount, Currency.getInstance(currencyCode));
    }
    public static MoneyEmbeddable from(Money m) {
        return new MoneyEmbeddable(m.amount(), m.currency().getCurrencyCode());
    }
}

@Entity
public class Order {
    @Id private UUID id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount",       column = @Column(name = "total_amount")),
        @AttributeOverride(name = "currencyCode", column = @Column(name = "currency"))
    })
    private MoneyEmbeddable totalAmount;

    public Money totalAmount() { return totalAmount.toDomain(); }
}
```

For tables where many money columns share **one** currency column (e.g. `order_lines` joined via `orders.currency`, or `pnl_daily.currency`), don't repeat the embeddable — store `BigDecimal` directly and reconstitute `Money` in the mapping layer using the row's currency.

### R2DBC (WebFlux services — `sync-api`, `analytics-api`)

R2DBC has no `@Embeddable`. We use a `BiFunction<Row, RowMetadata, Entity>` row mapper that reads both columns:

```java
public class OrderRowMapper implements BiFunction<Row, RowMetadata, Order> {
    @Override
    public Order apply(Row row, RowMetadata meta) {
        Currency ccy = Currency.getInstance(row.get("currency", String.class));
        BigDecimal total = row.get("total_amount", BigDecimal.class);
        return Order.builder()
            .id(row.get("id", UUID.class))
            .totalAmount(new Money(total, ccy))
            .placedAt(row.get("placed_at", OffsetDateTime.class).toInstant())
            .build();
    }
}
```

Writes go through a small helper that destructures `Money` into two parameters:

```java
public Mono<Void> insert(Order order) {
    return databaseClient.sql("""
        INSERT INTO orders (id, ebay_account_id, total_amount, currency, placed_at, ...)
        VALUES (:id, :acc, :total, :ccy, :placed, ...)
        """)
        .bind("id", order.id())
        .bind("acc", order.ebayAccountId())
        .bind("total", order.totalAmount().amount())
        .bind("ccy",   order.totalAmount().currency().getCurrencyCode())
        .bind("placed", order.placedAt())
        .then();
}
```

### Constraints we enforce at the DB level

```sql
-- Currency must be a known ISO code (cheap CHECK; full table lookup is overkill).
ALTER TABLE orders        ADD CONSTRAINT orders_currency_iso
  CHECK (currency ~ '^[A-Z]{3}$');

-- Amounts that should never be negative (revenue, units, stock):
ALTER TABLE pnl_daily ADD CONSTRAINT pnl_revenue_nonneg CHECK (revenue >= 0);
ALTER TABLE pnl_daily ADD CONSTRAINT pnl_units_nonneg   CHECK (units_sold >= 0);

-- Amounts that CAN be negative (net_profit, finance_events.amount) get no CHECK — losses and refunds are legitimate negatives.
```

### Cross-currency rows

A single row never mixes currencies. If we need to aggregate across currencies (a multi-marketplace seller running both EBAY_US and EBAY_DE), the conversion happens **in `accounting-api`** using `fx_rates` from a separate table, never via implicit casts. The result is stored in the tenant's **reporting currency** (`tenants.reporting_currency CHAR(3) NOT NULL DEFAULT 'USD'`) — original-currency rows are kept too. The default is USD because that's the product's base currency (see [MONETIZATION.md](MONETIZATION.md)); tenants may pick any ISO 4217 code at onboarding.

```sql
CREATE TABLE fx_rates (
  rate_date    DATE NOT NULL,
  from_ccy     CHAR(3) NOT NULL,
  to_ccy       CHAR(3) NOT NULL,
  rate         NUMERIC(18,10) NOT NULL,    -- BigDecimal, not Money
  source       TEXT NOT NULL,              -- 'ecb', 'ebay', ...
  PRIMARY KEY (rate_date, from_ccy, to_ccy, source)
);
```

## Data isolation

Every row that is tenant-owned has a `tenant_id`. We enforce isolation **two ways**:

1. Application layer — every repository takes a `TenantContext` and adds `WHERE tenant_id = :ctx` automatically (Spring Data interceptor / R2DBC `BiFunction`).
2. Row-level security on the most sensitive tables (`orders`, `finance_events`, `ebay_accounts`):

```sql
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
CREATE POLICY orders_tenant_isolation ON orders
  USING (tenant_id::text = current_setting('app.tenant_id', true));
```

Each connection sets `SET app.tenant_id = '<uuid>'` at the start of a request. RLS is our second line of defense if a bug forgets the `WHERE` clause.

## Encryption at rest

- eBay refresh tokens and TOTP secrets are encrypted column-side with AES-256-GCM. The master key lives in a env-loaded KMS file (later: Hashicorp Vault).
- DB-level encryption is provided by the file system on the Hetzner box (LUKS).

## Backups

- `pg_dump` daily + 7-day point-in-time recovery via WAL archiving to a Hetzner Storage Box.
- Weekly snapshot replicated to a second provider (e.g. Backblaze B2) for disaster recovery.
- Backup restore drill quarterly. Untested backups are not backups.

## Migrations

- Flyway, versioned `V001__init.sql`, `V002__add_inventory.sql`, ...
- Migrations are **forward-only**. To revert, write a new migration.
- Every PR that touches schema must include a migration + a Testcontainers test that runs against an empty DB.
