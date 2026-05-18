package com.ebaysoft.web.money;

import com.ebaysoft.domain.money.Money;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;

/**
 * Emits Money as {@code {"amount":"123.45","currency":"USD"}}. Amount is always a JSON string —
 * JavaScript parses JSON numbers as IEEE-754 doubles, which would silently corrupt totals at the
 * SPA boundary. See docs/BACKEND.md → Money handling.
 */
public final class MoneySerializer extends StdSerializer<Money> {

  public MoneySerializer() {
    super(Money.class);
  }

  @Override
  public void serialize(Money value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeStartObject();
    gen.writeStringField("amount", value.amount().toPlainString());
    gen.writeStringField("currency", value.currency().getCurrencyCode());
    gen.writeEndObject();
  }
}
