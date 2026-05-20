package com.ebaysoft.accounting.pnl;

import com.ebaysoft.domain.money.Money;

/**
 * Pure-function P&amp;L math. Lives outside any DB / Spring code so it's trivially testable.
 *
 * <p>{@code net = revenue − fees − refunds − cogs − shipping − ads}. All operands MUST share the
 * same currency — {@link Money} enforces that.
 */
public final class PnlCalculator {

  private PnlCalculator() {}

  public static Money net(Money revenue, Money fees, Money refunds, Money cogs, Money shipping, Money ads) {
    return revenue.minus(fees).minus(refunds).minus(cogs).minus(shipping).minus(ads);
  }

  public static Pnl withNet(
      java.util.UUID tenantId,
      java.time.LocalDate date,
      String groupBy,
      String groupKey,
      Money revenue,
      Money fees,
      Money refunds,
      Money cogs,
      Money shipping,
      Money ads) {
    return new Pnl(
        tenantId, date, groupBy, groupKey, revenue, fees, refunds, cogs, shipping, ads,
        net(revenue, fees, refunds, cogs, shipping, ads));
  }
}
