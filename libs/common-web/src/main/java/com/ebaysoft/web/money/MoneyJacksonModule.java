package com.ebaysoft.web.money;

import com.ebaysoft.domain.money.Money;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Single Jackson module that wires both the {@link MoneySerializer} and {@link MoneyDeserializer}.
 * Services typically don't register this directly — they pick it up via {@link
 * MoneyJacksonAutoConfiguration}.
 */
public final class MoneyJacksonModule extends SimpleModule {

  public MoneyJacksonModule() {
    super("ebay-soft.money");
    addSerializer(Money.class, new MoneySerializer());
    addDeserializer(Money.class, new MoneyDeserializer());
  }
}
