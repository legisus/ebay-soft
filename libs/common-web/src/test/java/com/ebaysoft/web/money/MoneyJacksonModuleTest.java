package com.ebaysoft.web.money;

import static org.assertj.core.api.Assertions.assertThat;

import com.ebaysoft.domain.money.Money;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class MoneyJacksonModuleTest {

  @Test
  void module_registers_both_codecs() throws Exception {
    ObjectMapper mapper = new ObjectMapper().registerModule(new MoneyJacksonModule());
    Money original = Money.of("42.00", "USD");
    String json = mapper.writeValueAsString(original);
    Money roundtrip = mapper.readValue(json, Money.class);
    assertThat(roundtrip).isEqualTo(original);
    assertThat(json).contains("\"amount\":\"42.00\"").contains("\"currency\":\"USD\"");
  }
}
