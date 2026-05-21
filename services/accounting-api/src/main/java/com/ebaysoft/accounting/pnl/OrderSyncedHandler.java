package com.ebaysoft.accounting.pnl;

import com.ebaysoft.domain.money.Money;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maps an {@code order.synced.v1} CloudEvent payload onto a {@code pnl_daily} row and persists it
 * idempotently. The accounting-api half of issue #49.
 *
 * <p>What's <b>wired up</b> here:
 *
 * <ul>
 *   <li>Pure mapping from the internal {@link OrderSyncedPayload} into a {@link Pnl} row via
 *       {@link PnlCalculator#withNet}.
 *   <li>Idempotent persistence via {@link PnlRepository#upsertFromEvent} — the {@code last_event_id}
 *       column in {@code pnl_daily} stops a redelivered event from double-counting.
 * </ul>
 *
 * <p>What's <b>not wired up yet</b> (each is its own follow-up commit):
 *
 * <ul>
 *   <li>The Postgres NOTIFY consumer that pulls events off the outbox and calls this handler.
 *       The {@link com.ebaysoft.events.outbox.OutboxEventPublisher} publish side landed in
 *       <code>95ac867</code>; the consumer side is the open architectural question (reactive vs
 *       blocking) on issue #49.
 *   <li>The COGS lookup. Today the handler passes {@code Money.zero} for cogs; the real version
 *       reads from {@code cogs_entries} keyed on the SKUs in the order. Lives behind a TODO
 *       below — small follow-up once order payloads carry SKU breakdowns.
 *   <li>The sync-api side that emits {@code order.synced.v1} into its outbox in the first place
 *       (it currently emits via the noop publisher per <code>EventPublisherConfig</code>).
 * </ul>
 *
 * <p>Until the consumer ships, this class is reachable only via integration tests — it exists so
 * the handler logic is reviewable + testable in isolation from the transport.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSyncedHandler {

  private final PnlRepository repo;

  /**
   * Process one {@code order.synced.v1} event. {@code eventId} is the CloudEvent id — also the
   * idempotency key.
   */
  @Transactional
  public void handle(OrderSyncedPayload payload, String eventId) {
    // Group by "day" / per-day key — matches the existing pnl_daily grouping. Listing/category/
    // month/year rollups land as parallel handlers when their queries land.
    Pnl row =
        PnlCalculator.withNet(
            payload.tenantId(),
            payload.orderDate(),
            "day",
            payload.orderDate().toString(),
            payload.revenue(),
            payload.fees(),
            payload.refunds(),
            // TODO #49 follow-up: look up COGS from cogs_entries by sku + effective_from ≤ orderDate.
            Money.zero(payload.revenue().currency()),
            payload.shipping(),
            payload.ads());

    int rows = repo.upsertFromEvent(row, eventId);
    log.atInfo()
        .addKeyValue("tenantId", payload.tenantId())
        .addKeyValue("orderId", payload.orderId())
        .addKeyValue("eventId", eventId)
        .addKeyValue("rowsAffected", rows)
        .log("order.synced applied to pnl_daily");
  }

  /**
   * Internal payload the handler consumes — services upstream (sync-api) marshal the raw eBay
   * shape into this before publishing. Keeps the handler decoupled from the eBay JSON we don't
   * control.
   */
  public record OrderSyncedPayload(
      UUID tenantId,
      LocalDate orderDate,
      Money revenue,
      Money fees,
      Money refunds,
      Money shipping,
      Money ads,
      String marketplaceId,
      String orderId) {}
}
