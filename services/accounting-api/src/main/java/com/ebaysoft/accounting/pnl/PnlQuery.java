package com.ebaysoft.accounting.pnl;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Parameters for a P&L query. Validates the {@code groupBy} value up front so the SQL builder
 * can't be tricked into emitting an unexpected column reference.
 */
public record PnlQuery(UUID tenantId, LocalDate from, LocalDate to, String groupBy) {

  public static final Set<String> VALID_GROUPS = Set.of("day", "month", "year", "listing", "category");

  public PnlQuery {
    if (tenantId == null) throw new IllegalArgumentException("tenantId required");
    if (from == null || to == null) throw new IllegalArgumentException("from/to required");
    if (from.isAfter(to)) throw new IllegalArgumentException("from must be ≤ to");
    if (groupBy == null || !VALID_GROUPS.contains(groupBy)) {
      throw new IllegalArgumentException("groupBy must be one of " + VALID_GROUPS);
    }
  }
}
