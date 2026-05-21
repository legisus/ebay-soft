package com.ebaysoft.accounting.cogs;

import com.ebaysoft.domain.money.Money;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One row of the per-SKU cost-of-goods-sold ledger. Each entry is the cost-as-of an
 * {@code effective_from} date — the most recent entry whose date is ≤ the order date is the
 * COGS used for P&amp;L on that order. History stays, so retroactive corrections never lose data.
 */
public record Cogs(UUID tenantId, String skuCode, Money cost, LocalDate effectiveFrom) {}
