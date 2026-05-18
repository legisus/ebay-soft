package com.ebaysoft.web.money;

import static org.assertj.core.api.Assertions.assertThat;

import com.ebaysoft.domain.money.Money;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MoneyJsonCodecTest {

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    mapper =
        new ObjectMapper()
            .registerModule(
                new SimpleModule()
                    .addSerializer(Money.class, new MoneySerializer())
                    .addDeserializer(Money.class, new MoneyDeserializer()));
  }

  @Test
  void serializes_money_with_amount_as_string_and_currency_as_iso_code() throws Exception {
    String json = mapper.writeValueAsString(Money.of("1243.59", "USD"));
    // The wire contract from docs/BACKEND.md.
    assertThat(json).isEqualTo("{\"amount\":\"1243.59\",\"currency\":\"USD\"}");
  }

  @Test
  void deserializes_money_from_the_documented_wire_shape() throws Exception {
    Money m = mapper.readValue("{\"amount\":\"12.50\",\"currency\":\"EUR\"}", Money.class);
    assertThat(m).isEqualTo(Money.of("12.50", "EUR"));
  }

  @Test
  void serializes_amount_with_currency_default_fraction_digits_no_scientific_notation() throws Exception {
    // 0.00000001 USD — must NOT come out as "1.0E-8"
    String json = mapper.writeValueAsString(new Money(new java.math.BigDecimal("0.00000001"), java.util.Currency.getInstance("USD")));
    // After HALF_EVEN normalization to USD's 2dp this rounds to 0.00.
    assertThat(json).isEqualTo("{\"amount\":\"0.00\",\"currency\":\"USD\"}");
  }

  @Test
  void round_trip_preserves_value() throws Exception {
    Money original = Money.of("99.95", "GBP");
    String json = mapper.writeValueAsString(original);
    Money reconstituted = mapper.readValue(json, Money.class);
    assertThat(reconstituted).isEqualTo(original);
  }
}
