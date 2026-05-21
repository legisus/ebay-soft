package com.ebaysoft.gateway.config;

import java.util.Optional;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * KeyResolver for {@code RequestRateLimiter} — derives a per-client identity
 * from {@code X-Forwarded-For} (set by Traefik / Cloudflare) and falls back to
 * the direct socket address. Anonymous requests share the bucket {@code
 * "anonymous"} so a Traefik misconfiguration that drops the header doesn't
 * silently disable the limiter (it just bucket-lumps every client together).
 *
 * <p>Per-IP is appropriate for the pre-auth endpoints we currently throttle
 * (signup, login). Per-tenant keys land alongside the gateway JWT filter
 * follow-up to #162.
 */
@Configuration
public class RateLimiterConfig {

  @Bean
  public KeyResolver ipKeyResolver() {
    return exchange -> {
      var headers = exchange.getRequest().getHeaders();
      // Header priority — most trustworthy first:
      //   1. CF-Connecting-IP — Cloudflare's authoritative client IP. Always
      //      present when traffic comes through our CF zone, and Cloudflare
      //      strips spoofed copies from outside.
      //   2. True-Client-IP    — Cloudflare Enterprise + some other CDNs.
      //   3. X-Forwarded-For   — public chain, first entry is the original
      //      client per spec. Used only when CF headers are absent.
      //   4. socket remote      — local dev / direct hits.
      String clientIp =
          firstNonBlank(headers.getFirst("CF-Connecting-IP"))
              .or(() -> firstNonBlank(headers.getFirst("True-Client-IP")))
              .or(() ->
                  firstNonBlank(headers.getFirst("X-Forwarded-For"))
                      .map(h -> h.split(",", 2)[0].trim()))
              .orElseGet(
                  () ->
                      Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                          .map(addr -> addr.getAddress().getHostAddress())
                          .orElse("anonymous"));
      return Mono.just(clientIp);
    };
  }

  private static Optional<String> firstNonBlank(String value) {
    return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.trim());
  }
}
