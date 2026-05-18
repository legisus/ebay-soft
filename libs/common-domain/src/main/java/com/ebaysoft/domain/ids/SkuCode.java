package com.ebaysoft.domain.ids;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Seller-defined SKU. Normalized to upper-case, trimmed, max 64 chars. Allowed characters: {@code
 * [A-Z0-9_-]}. eBay's own item-id is separate (a long) — this is what the seller types.
 */
public record SkuCode(String value) {

  private static final Pattern ALLOWED = Pattern.compile("[A-Z0-9_-]+");

  public SkuCode {
    Objects.requireNonNull(value, "value");
    if (!ALLOWED.matcher(value).matches() || value.length() > 64) {
      throw new IllegalArgumentException("invalid SKU code: " + value);
    }
  }

  public static SkuCode of(String raw) {
    Objects.requireNonNull(raw, "raw");
    String normalized = raw.trim().toUpperCase(Locale.ROOT);
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("SKU code is empty");
    }
    return new SkuCode(normalized);
  }
}
