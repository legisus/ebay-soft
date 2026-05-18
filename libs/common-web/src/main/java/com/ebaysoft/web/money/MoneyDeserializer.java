package com.ebaysoft.web.money;

import com.ebaysoft.domain.money.Money;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Currency;

/** Reads {@code {"amount":"123.45","currency":"USD"}} back into a {@link Money}. */
public final class MoneyDeserializer extends StdDeserializer<Money> {

  public MoneyDeserializer() {
    super(Money.class);
  }

  @Override
  public Money deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
    JsonNode node = p.getCodec().readTree(p);
    JsonNode amount = node.get("amount");
    JsonNode currency = node.get("currency");
    if (amount == null || currency == null) {
      throw ctx.weirdStringException(
          node.toString(), Money.class, "expected {\"amount\":\"...\",\"currency\":\"...\"}");
    }
    return new Money(new BigDecimal(amount.asText()), Currency.getInstance(currency.asText()));
  }
}
