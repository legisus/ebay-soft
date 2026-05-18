package com.ebaysoft.sync.ebay;

import com.ebaysoft.sync.config.EbayClientProperties;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

/**
 * Per-tenant token bucket on every outbound eBay call. Tenants are identified by the
 * {@code X-Tenant-Id} propagated request header (set by the calling code, not by an HTTP client —
 * we use {@link reactor.util.context.Context} to read it from the reactor pipeline).
 *
 * <p>The bucket is in-process; Redis-backed buckets land if/when sync-api scales to >1 instance.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EbayRateLimitFilter {

  static final String TENANT_CTX_KEY = "ebay-soft.tenant";
  private static final String DEFAULT_TENANT = "no-tenant";

  private final EbayClientProperties props;
  private final ConcurrentMap<String, RateLimiter> perTenant = new ConcurrentHashMap<>();

  public ExchangeFilterFunction asExchangeFilter() {
    return (req, next) ->
        next.exchange(req)
            .deferContextual(ctx -> {
              String tenantId = ctx.getOrDefault(TENANT_CTX_KEY, DEFAULT_TENANT);
              RateLimiter limiter =
                  perTenant.computeIfAbsent(tenantId, this::buildLimiter);
              return next.exchange(req).transformDeferred(RateLimiterOperator.of(limiter));
            });
  }

  private RateLimiter buildLimiter(String tenantId) {
    log.atInfo().addKeyValue("tenantId", tenantId).log("creating per-tenant eBay rate limiter");
    RateLimiterConfig cfg =
        RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .limitForPeriod((int) Math.ceil(props.perTenantPermitsPerSecond()))
            .timeoutDuration(Duration.ofSeconds(30))
            .build();
    return RateLimiterRegistry.of(cfg).rateLimiter("ebay-" + tenantId);
  }
}
