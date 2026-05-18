package com.ebaysoft.domain.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MoneyTest {

  private static final Currency USD = Currency.getInstance("USD");
  private static final Currency EUR = Currency.getInstance("EUR");
  private static final Currency JPY = Currency.getInstance("JPY");

  @Nested
  class Construction {

    @Test
    void of_constructs_money_from_string_and_currency_code() {
      Money m = Money.of("1.23", "USD");
      assertThat(m.amount()).isEqualByComparingTo(new BigDecimal("1.23"));
      assertThat(m.currency()).isEqualTo(USD);
    }

    @Test
    void constructor_normalizes_scale_to_currency_default_fraction_digits_with_half_even() {
      // HALF_EVEN: 1.235 → 1.24 (round to even, 4 is even)
      assertThat(new Money(new BigDecimal("1.235"), USD).amount())
          .isEqualByComparingTo(new BigDecimal("1.24"));
      // HALF_EVEN: 1.225 → 1.22 (round to even, 2 is even)
      assertThat(new Money(new BigDecimal("1.225"), USD).amount())
          .isEqualByComparingTo(new BigDecimal("1.22"));
      // JPY has 0 fraction digits → 100.6 → 101
      assertThat(new Money(new BigDecimal("100.6"), JPY).amount())
          .isEqualByComparingTo(new BigDecimal("101"));
    }

    @Test
    void zero_returns_money_with_zero_amount_in_given_currency() {
      Money z = Money.zero(USD);
      assertThat(z.amount()).isEqualByComparingTo(BigDecimal.ZERO);
      assertThat(z.currency()).isEqualTo(USD);
    }

    @Test
    void constructor_rejects_null_amount_or_currency() {
      assertThatThrownBy(() -> new Money(null, USD)).isInstanceOf(NullPointerException.class);
      assertThatThrownBy(() -> new Money(BigDecimal.ONE, null)).isInstanceOf(NullPointerException.class);
    }
  }

  @Nested
  class Arithmetic {

    @Test
    void plus_adds_amounts_within_same_currency() {
      Money result = Money.of("1.20", "USD").plus(Money.of("0.35", "USD"));
      assertThat(result).isEqualTo(Money.of("1.55", "USD"));
    }

    @Test
    void minus_subtracts_amounts_within_same_currency() {
      Money result = Money.of("1.50", "USD").minus(Money.of("0.30", "USD"));
      assertThat(result).isEqualTo(Money.of("1.20", "USD"));
    }

    @Test
    void negate_flips_sign() {
      assertThat(Money.of("1.50", "USD").negate()).isEqualTo(Money.of("-1.50", "USD"));
      assertThat(Money.of("-1.50", "USD").negate()).isEqualTo(Money.of("1.50", "USD"));
    }

    @Test
    void times_multiplies_by_a_scalar_factor() {
      Money result = Money.of("2.50", "USD").times(new BigDecimal("3"));
      assertThat(result).isEqualTo(Money.of("7.50", "USD"));
    }

    @Test
    void percent_treats_argument_as_whole_percentage_points() {
      // 100.00 × 19% = 19.00
      Money result = Money.of("100.00", "USD").percent(new BigDecimal("19"));
      assertThat(result).isEqualTo(Money.of("19.00", "USD"));
    }

    @Test
    void dividedBy_uses_half_even_and_currency_default_scale() {
      // 1.00 / 3 with HALF_EVEN @ 2dp = 0.33
      Money result = Money.of("1.00", "USD").dividedBy(new BigDecimal("3"));
      assertThat(result).isEqualTo(Money.of("0.33", "USD"));

      // 2.00 / 3 with HALF_EVEN @ 2dp = 0.67
      Money result2 = Money.of("2.00", "USD").dividedBy(new BigDecimal("3"));
      assertThat(result2).isEqualTo(Money.of("0.67", "USD"));
    }
  }

  @Nested
  class CurrencyMismatch {

    @Test
    void plus_throws_on_currency_mismatch() {
      assertThatThrownBy(() -> Money.of("1.00", "USD").plus(Money.of("1.00", "EUR")))
          .isInstanceOf(CurrencyMismatchException.class)
          .hasMessageContaining("USD")
          .hasMessageContaining("EUR");
    }

    @Test
    void minus_throws_on_currency_mismatch() {
      assertThatThrownBy(() -> Money.of("1.00", "USD").minus(Money.of("1.00", "EUR")))
          .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    void compareTo_throws_on_currency_mismatch() {
      assertThatThrownBy(() -> Money.of("1.00", "USD").compareTo(Money.of("1.00", "EUR")))
          .isInstanceOf(CurrencyMismatchException.class);
    }
  }

  @Nested
  class Comparison {

    @Test
    void compareTo_orders_by_amount_within_same_currency() {
      Money a = Money.of("1.00", "USD");
      Money b = Money.of("2.00", "USD");
      assertThat(a.compareTo(b)).isNegative();
      assertThat(b.compareTo(a)).isPositive();
      assertThat(a.compareTo(Money.of("1.00", "USD"))).isZero();
    }

    @Test
    void isNegative_true_only_when_amount_lt_zero() {
      assertThat(Money.of("-0.01", "USD").isNegative()).isTrue();
      assertThat(Money.zero(USD).isNegative()).isFalse();
      assertThat(Money.of("0.01", "USD").isNegative()).isFalse();
    }

    @Test
    void isZero_true_only_when_amount_eq_zero() {
      assertThat(Money.zero(USD).isZero()).isTrue();
      assertThat(Money.of("0.00", "USD").isZero()).isTrue();
      assertThat(Money.of("0.01", "USD").isZero()).isFalse();
    }
  }
}
