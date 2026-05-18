package com.ebaysoft.web.money;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration that contributes the {@link MoneyJacksonModule} to the
 * application's {@code ObjectMapper}. Any service that depends on {@code libs/common-web} gets
 * the codec wired automatically — no extra registration in user code.
 */
@AutoConfiguration(before = JacksonAutoConfiguration.class)
@ConditionalOnClass(name = "com.fasterxml.jackson.databind.ObjectMapper")
public class MoneyJacksonAutoConfiguration {

  @Bean
  public MoneyJacksonModule moneyJacksonModule() {
    return new MoneyJacksonModule();
  }
}
