package com.ebaysoft.accounting.pnl;

import com.ebaysoft.domain.money.Money;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One row of a profit-and-loss aggregation. The fundamental shape used everywhere {@code
 * accounting-api} exposes P&amp;L: a date (or aggregation key surrogate), a grouping label, and the
 * five money columns that sum to net.
 *
 * <p>{@code net = revenue − fees − refunds − cogs − shipping − ads}.
 * See docs/BACKEND.md → Money handling.
 */
public record Pnl(
    UUID tenantId,
    LocalDate date,
    String groupBy,        // "day" | "month" | "year" | "listing" | "category"
    String groupKey,
    Money revenue,
    Money fees,
    Money refunds,
    Money cogs,
    Money shipping,
    Money ads,
    Money net) {

  public static Pnl zero(UUID tenantId, LocalDate date, String groupBy, String groupKey, java.util.Currency currency) {
    Money z = Money.zero(currency);
    return new Pnl(tenantId, date, groupBy, groupKey, z, z, z, z, z, z, z);
  }
}
