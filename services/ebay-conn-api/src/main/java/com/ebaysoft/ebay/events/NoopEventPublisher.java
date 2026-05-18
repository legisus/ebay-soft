package com.ebaysoft.ebay.events;

import com.ebaysoft.events.cloudevent.CloudEvent;
import com.ebaysoft.events.publisher.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Fallback {@link EventPublisher} used when no real publisher is on the context — typically a
 * test profile that excluded R2DBC. Real boots get the {@link OutboxEventPublisher}.
 */
@Component
@ConditionalOnMissingBean(value = OutboxEventPublisher.class, ignored = NoopEventPublisher.class)
@Slf4j
public class NoopEventPublisher implements EventPublisher {

  @Override
  public void publish(CloudEvent event) {
    log.atDebug()
        .addKeyValue("type", event.type())
        .addKeyValue("subject", event.subject())
        .log("event swallowed by NoopEventPublisher");
  }
}
