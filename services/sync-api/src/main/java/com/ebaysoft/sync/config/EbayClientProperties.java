package com.ebaysoft.sync.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Tunables for sync-api's outbound calls to eBay. Defaults are conservative — eBay's published
 * limits are ~5000 calls/day per app for sandbox, 5k/day baseline for production (raised on
 * application). The token bucket prevents one chatty tenant from starving the others.
 */
@ConfigurationProperties(prefix = "ebay-soft.sync.ebay")
@Validated
public record EbayClientProperties(
    @NotBlank String baseUrl,
    @Positive int maxConnections,
    @NotBlank String connectTimeout,
    @NotBlank String readTimeout,
    @Positive double perTenantPermitsPerSecond,
    @Positive int perTenantBurst,
    @Positive int retryMaxAttempts,
    @NotBlank String retryBaseBackoff) {

  public Duration connectTimeoutDuration() {
    return Duration.parse(connectTimeout);
  }

  public Duration readTimeoutDuration() {
    return Duration.parse(readTimeout);
  }

  public Duration retryBaseBackoffDuration() {
    return Duration.parse(retryBaseBackoff);
  }

  public URI baseUri() {
    return URI.create(baseUrl);
  }
}
