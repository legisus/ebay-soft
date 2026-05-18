package com.ebaysoft.sync.status;

import java.time.Duration;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Sync status surface. {@code GET /v1/sync/status} returns the current snapshot;
 * {@code GET /v1/sync/status/stream} is the SSE feed the SPA subscribes to for live progress.
 *
 * <p>Issue #48. Real data wiring lands when the backfill pipeline ships (#42).
 */
@RestController
@RequestMapping("/v1/sync/status")
public class SyncStatusController {

  @GetMapping
  public Mono<SyncStatus> snapshot(@RequestParam("tenantId") UUID tenantId) {
    return Mono.just(SyncStatus.idle(tenantId));
  }

  /**
   * SSE feed. While the backfill is running this would push one event per progress update; the
   * idle stub emits a heartbeat every 15s so the connection stays warm and the SPA renders
   * "connected, idle".
   */
  @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<SyncStatus> stream(@RequestParam("tenantId") UUID tenantId) {
    return Flux.interval(Duration.ZERO, Duration.ofSeconds(15))
        .map(ignored -> SyncStatus.idle(tenantId));
  }
}
