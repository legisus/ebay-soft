package com.ebaysoft.accounting.pnl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ebaysoft.domain.money.CurrencyMismatchException;
import com.ebaysoft.domain.money.Money;
import org.junit.jupiter.api.Test;

class PnlCalculatorTest {

  @Test
  void net_subtracts_all_cost_components_from_revenue() {
    Money net = PnlCalculator.net(
        Money.of("1000.00", "USD"),   // revenue
        Money.of("100.00", "USD"),    // fees
        Money.of("50.00", "USD"),     // refunds
        Money.of("400.00", "USD"),    // cogs
        Money.of("30.00", "USD"),     // shipping
        Money.of("20.00", "USD"));    // ads
    // 1000 - 100 - 50 - 400 - 30 - 20 = 400
    assertThat(net).isEqualTo(Money.of("400.00", "USD"));
  }

  @Test
  void net_handles_zeros() {
    Money zero = Money.of("0.00", "USD");
    Money net = PnlCalculator.net(zero, zero, zero, zero, zero, zero);
    assertThat(net.isZero()).isTrue();
  }

  @Test
  void net_can_go_negative_when_costs_exceed_revenue() {
    Money net = PnlCalculator.net(
        Money.of("100.00", "USD"),
        Money.of("50.00", "USD"),
        Money.of("0.00", "USD"),
        Money.of("80.00", "USD"),
        Money.of("0.00", "USD"),
        Money.of("0.00", "USD"));
    assertThat(net).isEqualTo(Money.of("-30.00", "USD"));
    assertThat(net.isNegative()).isTrue();
  }

  @Test
  void cross_currency_inputs_are_rejected_at_the_Money_boundary() {
    assertThatThrownBy(() -> PnlCalculator.net(
            Money.of("100.00", "USD"),
            Money.of("10.00", "EUR"),   // mismatch
            Money.of("0.00", "USD"),
            Money.of("0.00", "USD"),
            Money.of("0.00", "USD"),
            Money.of("0.00", "USD")))
        .isInstanceOf(CurrencyMismatchException.class);
  }
}
