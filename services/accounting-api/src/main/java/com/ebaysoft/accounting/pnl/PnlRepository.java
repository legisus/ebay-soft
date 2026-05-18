package com.ebaysoft.accounting.pnl;

import com.ebaysoft.domain.money.Money;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Currency;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Read-side for {@code accounting.pnl_daily}. The write side is owned by the event consumer
 * (see {@code OrderSyncedEventHandler}) which upserts on each incoming {@code order.synced.v1}.
 */
@Repository
@RequiredArgsConstructor
public class PnlRepository {

  private final JdbcTemplate jdbc;

  public List<Pnl> find(PnlQuery q) {
    String sql =
        """
        SELECT tenant_id, date, group_by, group_key, currency,
               revenue, fees, refunds, cogs, shipping, ads, net
        FROM pnl_daily
        WHERE tenant_id = ?
          AND date BETWEEN ? AND ?
          AND group_by = ?
        ORDER BY date ASC, group_key ASC
        """;
    return jdbc.query(sql, ROW_MAPPER, q.tenantId(), q.from(), q.to(), q.groupBy());
  }

  /** Idempotent upsert keyed by the source event id; used by the event-driven write path. */
  public int upsertFromEvent(Pnl row, String eventId) {
    String sql =
        """
        INSERT INTO pnl_daily
            (tenant_id, date, group_by, group_key, currency,
             revenue, fees, refunds, cogs, shipping, ads, net, last_event_id, updated_at)
        VALUES (?, ?, ?, ?, ?,  ?, ?, ?, ?, ?, ?, ?, ?, now())
        ON CONFLICT (tenant_id, date, group_by, group_key)
        DO UPDATE SET
            revenue       = pnl_daily.revenue       + EXCLUDED.revenue,
            fees          = pnl_daily.fees          + EXCLUDED.fees,
            refunds       = pnl_daily.refunds       + EXCLUDED.refunds,
            cogs          = pnl_daily.cogs          + EXCLUDED.cogs,
            shipping      = pnl_daily.shipping      + EXCLUDED.shipping,
            ads           = pnl_daily.ads           + EXCLUDED.ads,
            net           = pnl_daily.net           + EXCLUDED.net,
            last_event_id = EXCLUDED.last_event_id,
            updated_at    = now()
        WHERE pnl_daily.last_event_id IS DISTINCT FROM EXCLUDED.last_event_id
        """;
    return jdbc.update(
        sql,
        row.tenantId(), row.date(), row.groupBy(), row.groupKey(), row.revenue().currency().getCurrencyCode(),
        row.revenue().amount(), row.fees().amount(), row.refunds().amount(),
        row.cogs().amount(), row.shipping().amount(), row.ads().amount(), row.net().amount(),
        eventId);
  }

  private static final RowMapper<Pnl> ROW_MAPPER = PnlRepository::mapRow;

  private static Pnl mapRow(ResultSet rs, int rowNum) throws java.sql.SQLException {
    Currency currency = Currency.getInstance(rs.getString("currency"));
    return new Pnl(
        (java.util.UUID) rs.getObject("tenant_id"),
        rs.getObject("date", java.time.LocalDate.class),
        rs.getString("group_by"),
        rs.getString("group_key"),
        money(rs.getBigDecimal("revenue"), currency),
        money(rs.getBigDecimal("fees"), currency),
        money(rs.getBigDecimal("refunds"), currency),
        money(rs.getBigDecimal("cogs"), currency),
        money(rs.getBigDecimal("shipping"), currency),
        money(rs.getBigDecimal("ads"), currency),
        money(rs.getBigDecimal("net"), currency));
  }

  private static Money money(BigDecimal amount, Currency currency) {
    return new Money(amount == null ? BigDecimal.ZERO : amount, currency);
  }
}
