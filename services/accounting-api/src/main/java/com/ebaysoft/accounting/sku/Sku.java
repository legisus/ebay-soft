package com.ebaysoft.accounting.sku;

import java.time.Instant;
import java.util.UUID;

/**
 * Temporary SKU master record — basic enough to back the manual-COGS UI; full ownership moves to
 * inventory-api in Phase 3 (see docs/ROADMAP.md).
 */
public record Sku(UUID tenantId, String skuCode, String title, Instant createdAt) {}
