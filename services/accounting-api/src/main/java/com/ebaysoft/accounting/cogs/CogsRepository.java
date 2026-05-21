package com.ebaysoft.accounting.cogs;

import com.ebaysoft.domain.money.Money;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Read/write for the temporary cogs_entries table; will migrate to inventory-api in Phase 3. */
@Repository
@RequiredArgsConstructor
public class CogsRepository {

  private final JdbcTemplate jdbc;

  public void upsert(Cogs row) {
    jdbc.update(
        """
        INSERT INTO cogs_entries (tenant_id, sku_code, currency, cost, effective_from)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT (tenant_id, sku_code, effective_from) DO UPDATE
        SET currency = EXCLUDED.currency,
            cost = EXCLUDED.cost
        """,
        row.tenantId(),
        row.skuCode(),
        row.cost().currency().getCurrencyCode(),
        row.cost().amount(),
        row.effectiveFrom());
  }

  public List<Cogs> findByTenant(UUID tenantId) {
    return jdbc.query(
        """
        SELECT tenant_id, sku_code, currency, cost, effective_from
        FROM cogs_entries WHERE tenant_id = ?
        ORDER BY sku_code ASC, effective_from DESC
        """,
        ROW_MAPPER,
        tenantId);
  }

  public List<Cogs> findByTenantAndSku(UUID tenantId, String skuCode) {
    return jdbc.query(
        """
        SELECT tenant_id, sku_code, currency, cost, effective_from
        FROM cogs_entries WHERE tenant_id = ? AND sku_code = ?
        ORDER BY effective_from DESC
        """,
        ROW_MAPPER,
        tenantId,
        skuCode);
  }

  public int delete(UUID tenantId, String skuCode, LocalDate effectiveFrom) {
    return jdbc.update(
        "DELETE FROM cogs_entries WHERE tenant_id = ? AND sku_code = ? AND effective_from = ?",
        tenantId,
        skuCode,
        effectiveFrom);
  }

  private static final RowMapper<Cogs> ROW_MAPPER = CogsRepository::mapRow;

  private static Cogs mapRow(ResultSet rs, int rowNum) throws java.sql.SQLException {
    Currency currency = Currency.getInstance(rs.getString("currency"));
    BigDecimal cost = rs.getBigDecimal("cost");
    return new Cogs(
        (UUID) rs.getObject("tenant_id"),
        rs.getString("sku_code"),
        new Money(cost == null ? BigDecimal.ZERO : cost, currency),
        rs.getObject("effective_from", LocalDate.class));
  }
}
