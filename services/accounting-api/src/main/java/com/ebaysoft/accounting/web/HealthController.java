package com.ebaysoft.accounting.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

  @GetMapping("/v1/health")
  public Map<String, String> health() {
    return Map.of("status", "ok", "service", "accounting-api");
  }
}
