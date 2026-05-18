package com.ebaysoft.sync.events;

import com.ebaysoft.events.publisher.EventPublisher;
import com.ebaysoft.events.publisher.NoopEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fallback {@link EventPublisher} bean. Real boots will also register a primary R2DBC-backed
 * publisher (lands in a follow-up alongside the outbox forwarder); until then this is what wires.
 */
@Configuration
public class EventPublisherConfig {

  @Bean
  @ConditionalOnMissingBean(EventPublisher.class)
  public EventPublisher noopEventPublisher() {
    return new NoopEventPublisher();
  }
}
