package com.ebaysoft.sync.status;

import java.time.Instant;
import java.util.UUID;

/**
 * Public shape of {@code GET /v1/sync/status} — the SPA polls or subscribes (SSE) to render the
 * "syncing…" progress bar after a seller connects their eBay account.
 */
public record SyncStatus(
    UUID tenantId,
    String stream,                  // "orders" | "listings" | "finance" | "all"
    long itemsProcessed,
    Long itemsExpected,             // null until eBay tells us a total
    Instant watermark,              // last successful event time per stream
    String phase                    // "queued" | "backfilling" | "incremental" | "idle" | "failed"
) {

  public static SyncStatus idle(UUID tenantId) {
    return new SyncStatus(tenantId, "all", 0, null, null, "idle");
  }
}
