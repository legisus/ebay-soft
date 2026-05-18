# Backend — Java 25 LTS + Spring Boot, REST services

ebay-soft's backend is a set of **dedicated REST services**, not a monolith. Each service is its own Spring Boot application with its own runnable jar, Docker image, deploy lifecycle, OpenAPI contract, and Postgres schema. They communicate over HTTP via generated OpenAPI clients and (for events) Postgres `LISTEN/NOTIFY`. See [ARCHITECTURE.md](ARCHITECTURE.md) for the service list and contracts.

## Stack defaults per service

- **Spring MVC + virtual threads** (`spring.threads.virtual.enabled=true`) is the default. With Java 21+ virtual threads, blocking I/O is cheap and MVC code is easier to read, test, and debug.
- **Spring WebFlux + R2DBC** is used only where the workload genuinely benefits: `ebay-conn-api`, `sync-api`, `analytics-api`. Any service that needs to fan out thousands of outbound calls or stream SSE goes reactive.
- The choice is per-service, not per-app. We never mix both web stacks in the same Spring Boot application.

## Versions

| Component        | Version (target)          | Notes                                                                 |
|------------------|---------------------------|-----------------------------------------------------------------------|
| Java             | 25 LTS                    | Released Sept 2025, support until 2033. Use virtual threads selectively. |
| Spring Boot      | 3.5.x (latest stable)     | Spring Framework 6.2 line. Native-image friendly.                     |
| Spring Security  | 6.4.x                     | OAuth2 client for eBay, JWT resource-server for our SPA               |
| Project Reactor  | 3.7.x                     | Pulled in by WebFlux                                                  |
| R2DBC Postgres   | 1.0.x                     | For reactive modules                                                  |
| Flyway           | 11.x                      | Migrations                                                            |
| Testcontainers   | 1.20.x                    | Postgres + Wiremock + Redis                                           |
| Lombok           | latest                    | For `@Slf4j`, `@Builder`, `@Value` — keep usage modest                |
| MapStruct        | 1.6.x                     | DTO ↔ entity mapping                                                  |

## Project layout (Gradle monorepo, one Spring Boot app per service)

```
ebay-soft/
├── settings.gradle.kts              # includes every service + lib
├── build.gradle.kts                 # root: convention plugins, version catalog
├── gradle/libs.versions.toml        # single source of truth for versions
├── buildSrc/                        # custom convention plugins (java-service, webflux-service, ...)
├── platform/                        # shared BOM, dependency constraints
├── libs/
│   ├── common-domain/               # value objects (Money, TenantId, SkuCode) — pure POJOs, zero Spring
│   ├── common-web/                  # gateway-header parsing, error model, OpenAPI config, OTel setup
│   ├── common-security/             # service-account JWT, JWKS client
│   ├── common-events/               # CloudEvents envelope, LISTEN/NOTIFY publisher/consumer
│   └── common-test/                 # Testcontainers fixtures, Wiremock helpers, MockEbay
├── services/
│   ├── auth-api/                    # Spring MVC + virtual threads
│   ├── ebay-conn-api/               # Spring WebFlux
│   ├── sync-api/                    # Spring WebFlux + R2DBC
│   ├── accounting-api/              # Spring MVC + virtual threads
│   ├── inventory-api/               # Spring MVC + virtual threads
│   ├── repricer-api/                # Spring MVC + virtual threads
│   ├── analytics-api/               # Spring WebFlux + R2DBC + SSE
│   ├── notif-api/                   # Spring MVC + virtual threads
│   ├── billing-api/                 # Spring MVC + virtual threads (Stripe)
│   ├── admin-api/                   # Spring MVC + virtual threads
│   └── api-gateway/                 # Spring Cloud Gateway (reactive)
├── clients/                         # generated from each service's openapi.yaml — published to local Maven repo
│   ├── auth-api-client/
│   ├── ebay-conn-api-client/
│   └── ...
└── ml-api/                          # Python FastAPI service, own pyproject.toml, separate CI lane
```

Why monorepo over multi-repo:

- One PR can land a contract change in both the provider and its consumers; contract tests run on the same checkout.
- Version catalog forces one Spring Boot / Java version across all services — no drift.
- Gradle's task graph + `--configure-on-demand` only rebuilds services whose sources actually changed.
- Easier shared-lib bumps; harder to accidentally fork.

What we resist:

- **No** "common-business-logic" lib. Shared libs are infrastructure only. Each service owns its domain.
- **No** shared entity classes across services. If two services need the same shape, generate it from one's OpenAPI spec.

## Each service has

- Its own `application.yml` and `application-prod.yml`.
- Its own Flyway migrations, run on startup, against its own Postgres schema.
- Its own Docker image (`Dockerfile` per service, Jib or Buildpacks).
- Its own `openapi.yaml` checked in to source, **kept in sync** with the running app via a CI check that fails if `GET /openapi.json` doesn't match the committed spec.
- A published `*-client` artifact built from its OpenAPI spec via `openapi-generator`. Consumers depend on this jar; nobody hand-writes a client.
- A `Dockerfile`, a `compose.partial.yml` snippet, and a `README.md` describing how to run it standalone.

## Deployment unit per service

```
services/accounting-api/
├── src/main/java/com/ebaysoft/accounting/...
├── src/main/resources/
│   ├── application.yml
│   ├── application-prod.yml
│   └── db/migration/                # Flyway: V001__init.sql, ...
├── src/test/...
├── openapi.yaml                     # contract — single source of truth
├── Dockerfile
├── compose.partial.yml              # merged into root compose.yml
└── build.gradle.kts
```

## Logging — `@Slf4j` conventions

```java
@Slf4j
@Service
public class EbayOrderSyncService {

    public Mono<SyncResult> syncOrders(SellerId sellerId, Instant since) {
        log.atInfo()
           .addKeyValue("sellerId", sellerId.value())
           .addKeyValue("since", since)
           .log("starting order sync");
        return ebayClient.fetchOrders(sellerId, since)
            .doOnNext(o -> log.atDebug()
                .addKeyValue("orderId", o.id())
                .log("fetched order"))
            .doOnError(e -> log.atError()
                .addKeyValue("sellerId", sellerId.value())
                .setCause(e)
                .log("sync failed"));
    }
}
```

Rules:

- Use the **fluent API** (`log.atInfo().addKeyValue(...)`) so logs are structured. Logback's JSON encoder will emit them as proper fields, which makes Loki/Grafana queries painless.
- **Never** log access tokens, refresh tokens, customer emails, or addresses. We add a Logback `MaskingConverter` and a unit test that fails the build if a known-secret pattern appears in a log line.
- Levels: `ERROR` = pager-worthy. `WARN` = recoverable but suspicious. `INFO` = business-meaningful event (sync started, payment captured). `DEBUG` = developer-only, off in prod.

## Money handling

ebay-soft is a financial dashboard. **`float` and `double` are forbidden for monetary amounts** — binary floating point can't represent decimal fractions exactly (`0.1 + 0.2 == 0.30000000000000004`), which silently produces wrong P&L after a few thousand orders. There is no "small enough" use case for `double` here.

### Rules

1. Money is always **`BigDecimal`** at rest in Java, or wrapped in our **`Money`** value type (preferred).
2. Every `BigDecimal` division specifies **scale and `RoundingMode` explicitly** — no defaults.
3. Default rounding mode is **`RoundingMode.HALF_EVEN`** (banker's rounding) for accounting. Tax math may use `HALF_UP` when a jurisdiction requires it; document the choice at the call site.
4. Never construct `new BigDecimal(double)` — use `new BigDecimal(String)` or `BigDecimal.valueOf(double)`.
5. Money crosses the wire as a **JSON string**, never a JSON number — JS parses numbers as IEEE-754 doubles and would re-introduce the bug at the SPA boundary.
6. Currencies are ISO 4217 `Currency` instances. Cross-currency arithmetic throws.

### The `Money` type — in `libs/common-domain`

```java
public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN);
    }

    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money plus(Money other)  { requireSameCurrency(other); return new Money(amount.add(other.amount), currency); }
    public Money minus(Money other) { requireSameCurrency(other); return new Money(amount.subtract(other.amount), currency); }
    public Money negate()           { return new Money(amount.negate(), currency); }

    public Money times(BigDecimal factor) {
        return new Money(amount.multiply(factor), currency);
    }

    public Money percent(BigDecimal percentage) {
        return times(percentage.movePointLeft(2));   // 19% → ×0.19
    }

    public Money dividedBy(BigDecimal divisor) {
        return new Money(amount.divide(divisor,
                currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN), currency);
    }

    public boolean isNegative() { return amount.signum() < 0; }
    public boolean isZero()     { return amount.signum() == 0; }

    @Override public int compareTo(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount);
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency))
            throw new CurrencyMismatchException(currency, other.currency);
    }
}
```

Why a custom record instead of Joda-Money or JavaMoney (JSR 354):

- It's ~50 lines we own and can evolve with our needs (e.g. attaching tax behavior later).
- No transitive dep, no surprises.
- We don't need pluggable FX strategies (`accounting-api` handles conversion explicitly via daily ECB rates).

Joda-Money is a fine drop-in alternative if you'd rather not own the type — same author as `java.time`.

### Database mapping

PostgreSQL `NUMERIC(p,s)` maps cleanly to `BigDecimal`. The widths we standardize on (also enforced in [DATABASE.md](DATABASE.md)):

| Use                                       | Type             |
|-------------------------------------------|------------------|
| Per-line amounts (unit_price, fees, tax)  | `NUMERIC(12,2)`  |
| Aggregates (period revenue, tenant LTV)   | `NUMERIC(14,2)`  |
| Percentages / rates                       | `NUMERIC(7,6)`   |
| FX rates                                  | `NUMERIC(18,10)` |

R2DBC/JDBC return `BigDecimal` natively. The `Money` value gets reconstituted by an attribute converter (`AttributeConverter<Money, BigDecimal>` for JPA; a custom `Converter` for R2DBC), reading the currency from a sibling `CHAR(3)` column.

### Jackson configuration (every service)

In `libs/common-web`:

```java
@Configuration
public class JsonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer moneyJacksonCustomizer() {
        return builder -> builder
            .featuresToEnable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN)  // no scientific notation
            .serializerByType(BigDecimal.class, new ToStringSerializer())     // emit as JSON string
            .serializers(new MoneySerializer())                               // { amount: "12.34", currency: "USD" }
            .deserializers(new MoneyDeserializer());
    }
}
```

The wire format for any `Money` field is:

```json
{ "amount": "1243.59", "currency": "USD" }
```

The OpenAPI spec declares `amount` as `type: string, format: decimal`. The generated TypeScript client receives a `string`. The SPA formats with `Intl.NumberFormat` for display and uses `decimal.js` if it ever needs client-side arithmetic (rare — aggregations come from `accounting-api`).

### Forbidden idioms — caught in CI

A static-analysis rule (ArchUnit) fails the build on:

- Any field, parameter, or return type of `float` or `double` in code under `com.ebaysoft..` (except in explicitly allowlisted analytics / chart-coordinate helpers).
- Any use of `new BigDecimal(double)` — only the `String` constructor or `BigDecimal.valueOf` allowed.
- Any `BigDecimal#divide(BigDecimal)` without a `RoundingMode` argument.

```java
@ArchTest
static final ArchRule no_doubles_for_money =
    noClasses().that().resideInAPackage("com.ebaysoft..")
        .should().dependOnClassesThat().haveFullyQualifiedName("double")
        .orShould().dependOnClassesThat().haveFullyQualifiedName("float")
        .because("money is BigDecimal/Money, not floating point — see docs/BACKEND.md");
```

### Test discipline

Unit tests for any P&L / fee / tax math:

- Always assert with `BigDecimal.compareTo` (or `Money.equals` after normalization), never `equals` on raw `BigDecimal` (which considers `1.0` and `1.00` unequal).
- Always assert exact expected values, not approximate — there's no "close enough" in money math.
- A reconciliation test runs over a 12-month synthetic dataset and asserts that the sum of line-level P&L equals the period-level aggregate to the cent.

## WebFlux patterns we'll standardize on

### Outbound calls to eBay

```java
@Configuration
public class EbayWebClientConfig {

    @Bean
    public WebClient ebayWebClient(EbayProperties props) {
        ConnectionProvider pool = ConnectionProvider.builder("ebay")
            .maxConnections(200)
            .pendingAcquireMaxCount(2_000)
            .pendingAcquireTimeout(Duration.ofSeconds(30))
            .build();

        HttpClient http = HttpClient.create(pool)
            .responseTimeout(Duration.ofSeconds(30))
            .compress(true);

        return WebClient.builder()
            .baseUrl(props.baseUrl())
            .clientConnector(new ReactorClientHttpConnector(http))
            .defaultHeader(HttpHeaders.ACCEPT, "application/json")
            .filter(new EbayAuthFilter())          // injects Bearer token, refreshes if needed
            .filter(new EbayRateLimitFilter())     // respects per-seller daily quota
            .filter(new EbayRetryFilter())         // exponential backoff on 429/5xx
            .build();
    }
}
```

### Inbound endpoint (functional style)

```java
@Configuration
public class AnalyticsRoutes {

    @Bean
    public RouterFunction<ServerResponse> analyticsRoutes(AnalyticsHandler h) {
        return route()
            .GET("/api/analytics/pnl",        h::pnl)
            .GET("/api/analytics/top-skus",   h::topSkus)
            .GET("/api/analytics/stream",     accept(TEXT_EVENT_STREAM), h::stream)
            .build();
    }
}
```

SSE for live dashboard updates beats WebSocket for our use case: simpler, automatic reconnect, fewer infra concerns.

### Virtual threads

Java 25's virtual threads make blocking code cheap. Use them for the **non-reactive** modules (`auth`, `billing`) with `spring.threads.virtual.enabled=true`. Don't mix into reactive code — it defeats the point.

## Error handling

A single `@ControllerAdvice` that maps domain exceptions to RFC 7807 Problem Details:

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(EbayRateLimitedException.class)
    public ResponseEntity<ProblemDetail> rateLimited(EbayRateLimitedException ex) {
        log.atWarn().addKeyValue("retryAfter", ex.retryAfter()).log("ebay rate limited");
        ProblemDetail body = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        body.setTitle("eBay temporarily unavailable");
        body.setProperty("retryAfter", ex.retryAfter().toSeconds());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
```

## Testing strategy

The full strategy — pyramid, frameworks per layer, contract testing, ephemeral E2E envs, CI gates, mutation testing, flake policy — is in [TESTING.md](TESTING.md). Per-service shape inside this monorepo:

| Layer       | Tool                                    | What we test                                            |
|-------------|-----------------------------------------|---------------------------------------------------------|
| Unit        | JUnit 5 + AssertJ + Mockito             | Pure logic (P&L math, rule evaluation, mappers, Money arithmetic) |
| Slice       | `@WebMvcTest` / `@WebFluxTest` / `@DataR2dbcTest` | Single Spring layer in isolation                  |
| Integration | Testcontainers + WireMock               | Full request → DB path; external APIs (eBay/Stripe/Twilio) stubbed |
| Contract    | **Pact** (consumer-driven) + self-hosted Pact Broker | Every consumer/provider pair                |
| API E2E     | REST-assured                            | Cross-service flows through the gateway, ephemeral env  |
| UI E2E      | Playwright                              | Critical user journeys against the running stack         |
| Load        | Gatling                                 | Sync 100k orders, dashboard concurrent users, repricer batch |
| Mutation    | Pitest                                  | Weekly; catches tests that pass against arbitrary code   |

## Inter-service calls — pattern

```java
// generated by openapi-generator from inventory-api/openapi.yaml
public interface InventoryApiClient {
    @GetExchange("/v1/skus/{skuId}")
    SkuDto getSku(@PathVariable UUID skuId);
}

@Service
@Slf4j
@RequiredArgsConstructor
public class PnlService {
    private final InventoryApiClient inventory;          // service-to-service

    public Pnl computeFor(SkuId sku, DateRange range) {
        SkuDto info = inventory.getSku(sku.value());     // virtual thread parks on I/O
        // ... pure logic
    }
}
```

Wiring uses Spring 6.2's `HttpServiceProxyFactory` over `RestClient` (MVC services) or `WebClient` (reactive services). Every client:

- Has explicit timeouts (`2s connect / 5s read` default; tighter per call).
- Carries propagated `X-Tenant-Id`, `X-User-Id`, and `traceparent` headers automatically (interceptor in `common-web`).
- Wraps calls in a Resilience4j circuit breaker + retry policy (idempotent GETs only).
- Validates a service-account JWT on the receiving side; calls without it are rejected.

## Event publishing — pattern

```java
@Service
@RequiredArgsConstructor
public class EbayAccountService {
    private final EventPublisher events;        // from common-events

    public void onConnected(EbayAccount account) {
        events.publish(CloudEvent.builder()
            .type("ebay-soft.ebay_account.connected.v1")
            .source("/ebay-conn-api")
            .subject(account.tenantId().toString())
            .data(new EbayAccountConnected(account.id(), account.marketplaceId()))
            .build());
    }
}
```

`common-events` writes the event to an `outbox` table inside the publisher's own schema, then a poller forwards committed rows to Postgres `NOTIFY` (or NATS in Phase 2). This is the **transactional outbox** pattern — guarantees at-least-once delivery without losing events on a crash.

Consumers in other services subscribe via `LISTEN <topic>` and dedupe by event ID.

## Dependencies — opinionated short list

```kotlin
// app/build.gradle.kts (excerpt)
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:r2dbc-postgresql")
    runtimeOnly("org.postgresql:postgresql") // for Flyway

    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("net.logstash.logback:logstash-logback-encoder")

    implementation("org.mapstruct:mapstruct:1.6.3")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    implementation("com.stripe:stripe-java:28.0.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.wiremock:wiremock-standalone:3.9.1")
}
```

## API style

- REST + JSON for everything client-facing, documented via OpenAPI 3.1 (springdoc).
- SSE for live dashboard streams.
- No GraphQL at MVP — too much surface area for too little benefit when there's one client.

## Configuration discipline

The full secrets policy (public-repo discipline, sealed-file pattern with sops + age, GitHub Actions Secrets, leak response, rotation cadence) lives in [SECURITY.md → Secrets](SECURITY.md#secrets). Application code follows three rules:

- **`application.yml`** holds defaults that are safe to ship publicly. `application-{dev,stg,prod}.yml` holds non-secret per-env overrides.
- **Secrets are read only from environment variables** via `@Value("${...}")` or `@ConfigurationProperties`. No application code reads `.env` files, decrypts sops, or talks to Vault directly — that work is done by the deploy layer (Docker Compose + Ansible), which injects env vars into the running container.
- Each module ships its own `@ConfigurationProperties` record with `@Validated`, and **fails fast on startup** if a required secret env var is missing. Better to refuse to boot than to run silently with a `null` secret.

```java
@ConfigurationProperties(prefix = "ebay-soft.stripe")
@Validated
public record StripeProperties(
    @NotBlank String apiKey,            // STRIPE_API_KEY (env)
    @NotBlank String webhookSecret      // STRIPE_WEBHOOK_SECRET (env)
) {}
```

For multi-line secrets (PEM private keys), Spring reads the path from an env var and the content from disk — the file is mounted by Docker into `/run/secrets/<name>` from the decrypted sealed file.

```yaml
ebay-soft:
  jwt:
    private-key-path: ${JWT_PRIVATE_KEY_PATH:/run/secrets/jwt-private-key.pem}
```

**Never** put a secret literal in any YAML committed to the repo. Even for dev. Even temporarily. CI's gitleaks scan will catch and reject it.
