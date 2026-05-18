package com.ebaysoft.domain.money;

import java.util.Currency;

/**
 * Thrown when an arithmetic or comparison operation is attempted between two {@link Money} values
 * that carry different currencies. Cross-currency math must go through an explicit FX conversion in
 * {@code accounting-api} — see docs/BACKEND.md.
 */
public final class CurrencyMismatchException extends RuntimeException {

  public CurrencyMismatchException(Currency left, Currency right) {
    super("currency mismatch: " + left.getCurrencyCode() + " vs " + right.getCurrencyCode());
  }
}
