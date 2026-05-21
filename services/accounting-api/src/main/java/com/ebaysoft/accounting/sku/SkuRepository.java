package com.ebaysoft.accounting.sku;

import java.sql.ResultSet;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SkuRepository {

  private final JdbcTemplate jdbc;

  public void upsert(UUID tenantId, String skuCode, String title) {
    jdbc.update(
        """
        INSERT INTO skus (tenant_id, sku_code, title) VALUES (?, ?, ?)
        ON CONFLICT (tenant_id, sku_code) DO UPDATE SET title = EXCLUDED.title
        """,
        tenantId,
        skuCode,
        title);
  }

  public List<Sku> findByTenant(UUID tenantId) {
    return jdbc.query(
        "SELECT tenant_id, sku_code, title, created_at FROM skus WHERE tenant_id = ? ORDER BY created_at DESC",
        ROW_MAPPER,
        tenantId);
  }

  public int delete(UUID tenantId, String skuCode) {
    return jdbc.update("DELETE FROM skus WHERE tenant_id = ? AND sku_code = ?", tenantId, skuCode);
  }

  private static final RowMapper<Sku> ROW_MAPPER =
      (ResultSet rs, int n) ->
          new Sku(
              (UUID) rs.getObject("tenant_id"),
              rs.getString("sku_code"),
              rs.getString("title"),
              rs.getTimestamp("created_at").toInstant());
}
