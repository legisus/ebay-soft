package com.ebaysoft.ebay.events;

import com.ebaysoft.events.cloudevent.CloudEvent;
import com.ebaysoft.events.publisher.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Always-on fallback {@link EventPublisher}. Real boots also register {@link OutboxEventPublisher}
 * with {@code @Primary}, so this one is only chosen when R2DBC isn't available (lightweight tests).
 */
@Component
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
