package com.ebaysoft.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Smoke endpoint used by the gateway and Phase 0 deploy verification — confirms the service is up
 * before real domain endpoints exist. Removed once {@code POST /v1/auth/signup} ships.
 */
@RestController
public class HealthController {

  @GetMapping("/v1/health")
  @Operation(operationId = "getHealth", summary = "Smoke endpoint — service responding.")
  public Map<String, String> health() {
    return Map.of("status", "ok", "service", "auth-api");
  }
}
