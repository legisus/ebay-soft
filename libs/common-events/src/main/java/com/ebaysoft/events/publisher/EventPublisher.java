package com.ebaysoft.events.publisher;

import com.ebaysoft.events.cloudevent.CloudEvent;

/**
 * Publishes a CloudEvent through the transactional outbox. Implementations write to the publisher
 * service's own {@code outbox} table inside the current DB transaction, then a forwarder turns
 * those rows into {@code LISTEN/NOTIFY} messages (or, post-MVP, NATS publishes).
 */
public interface EventPublisher {

  /**
   * Atomic with the current transaction. If the transaction rolls back, the event isn't published —
   * that's the whole point of the outbox.
   */
  void publish(CloudEvent event);
}
