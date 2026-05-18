package com.ebaysoft.domain.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Money value type — see docs/BACKEND.md → Money handling.
 *
 * <p>Construction normalizes amount to the currency's default fraction digits using HALF_EVEN
 * (banker's rounding). Cross-currency arithmetic throws {@link CurrencyMismatchException}; FX
 * conversion is the job of {@code accounting-api}, not this type.
 */
public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {

  public Money {
    Objects.requireNonNull(amount, "amount");
    Objects.requireNonNull(currency, "currency");
    amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN);
  }

  public static Money of(String amount, String currencyCode) {
    return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
  }

  public static Money zero(Currency currency) {
    return new Money(BigDecimal.ZERO, currency);
  }

  public Money plus(Money other) {
    requireSameCurrency(other);
    return new Money(amount.add(other.amount), currency);
  }

  public Money minus(Money other) {
    requireSameCurrency(other);
    return new Money(amount.subtract(other.amount), currency);
  }

  public Money negate() {
    return new Money(amount.negate(), currency);
  }

  public Money times(BigDecimal factor) {
    return new Money(amount.multiply(factor), currency);
  }

  /** Multiplies by a whole-percent factor — 19 means 19%, not 0.19. */
  public Money percent(BigDecimal percentage) {
    return times(percentage.movePointLeft(2));
  }

  public Money dividedBy(BigDecimal divisor) {
    return new Money(
        amount.divide(divisor, currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN),
        currency);
  }

  public boolean isNegative() {
    return amount.signum() < 0;
  }

  public boolean isZero() {
    return amount.signum() == 0;
  }

  @Override
  public int compareTo(Money other) {
    requireSameCurrency(other);
    return amount.compareTo(other.amount);
  }

  private void requireSameCurrency(Money other) {
    if (!currency.equals(other.currency)) {
      throw new CurrencyMismatchException(currency, other.currency);
    }
  }
}
