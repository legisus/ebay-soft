package com.ebaysoft.analytics.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Phase-0 smoke endpoint. Removed once real {@code /v1/analytics/*} endpoints land. */
@RestController
public class HealthController {

  @GetMapping("/v1/health")
  public Map<String, String> health() {
    return Map.of("status", "ok", "service", "analytics-api");
  }
}
