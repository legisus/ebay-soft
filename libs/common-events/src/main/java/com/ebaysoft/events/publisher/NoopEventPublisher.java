package com.ebaysoft.events.publisher;

import com.ebaysoft.events.cloudevent.CloudEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * No-op {@link EventPublisher}. Services register one of these as a fallback so a Spring context
 * without a real outbox-backed publisher (typical for unit tests) still satisfies
 * {@code @Autowired EventPublisher}. Real boots register a service-local publisher with
 * {@code @Primary} so the no-op is never chosen.
 */
public final class NoopEventPublisher implements EventPublisher {

  private static final Logger LOG = Logger.getLogger(NoopEventPublisher.class.getName());

  @Override
  public void publish(CloudEvent event) {
    LOG.log(Level.FINE, "noop publisher swallowed event {0}", event.type());
  }
}
