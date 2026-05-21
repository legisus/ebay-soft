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
      String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
      String clientIp;
      if (forwarded != null && !forwarded.isBlank()) {
        // X-Forwarded-For is a comma-separated chain: client, proxy1, proxy2.
        // We want the original client, which is the first entry.
        clientIp = forwarded.split(",", 2)[0].trim();
      } else {
        clientIp =
            Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                .map(addr -> addr.getAddress().getHostAddress())
                .orElse("anonymous");
      }
      return Mono.just(clientIp);
    };
  }
}
