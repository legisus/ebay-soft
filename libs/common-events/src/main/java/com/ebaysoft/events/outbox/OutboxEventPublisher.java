package com.ebaysoft.events.outbox;

import com.ebaysoft.events.cloudevent.CloudEvent;
import com.ebaysoft.events.publisher.EventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.function.Supplier;

/**
 * JDBC-backed {@link EventPublisher} that writes each {@link CloudEvent} into the local
 * service's {@code outbox} table. Atomic with the current transaction: the caller passes a
 * {@code Supplier<Connection>} that returns the txn-scoped connection (e.g.,
 * {@code DataSourceUtils.getConnection(ds)} in Spring), so a rollback drops the row too.
 *
 * <p>The forwarder reads {@code forwarded_at IS NULL} rows in a separate step and emits the
 * Postgres NOTIFY — keeping publish-side latency at one insert.
 */
public final class OutboxEventPublisher implements EventPublisher {

  private static final String INSERT_SQL =
      """
      INSERT INTO outbox (event_id, type, source, subject, time, payload)
      VALUES (?, ?, ?, ?, ?, ?::jsonb)
      """;

  private final Supplier<Connection> connectionSupplier;
  private final ObjectMapper mapper;

  public OutboxEventPublisher(Supplier<Connection> connectionSupplier, ObjectMapper mapper) {
    this.connectionSupplier = connectionSupplier;
    this.mapper = mapper;
  }

  @Override
  public void publish(CloudEvent event) {
    String payload;
    try {
      payload = mapper.writeValueAsString(event.data());
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize CloudEvent payload to JSON", e);
    }
    try (PreparedStatement ps = connectionSupplier.get().prepareStatement(INSERT_SQL)) {
      ps.setString(1, event.id());
      ps.setString(2, event.type());
      ps.setString(3, event.source());
      ps.setString(4, event.subject());
      ps.setTimestamp(5, Timestamp.from(event.time()));
      ps.setString(6, payload);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new OutboxPublishException(
          "Failed to insert event " + event.id() + " into outbox: " + e.getMessage(), e);
    }
  }

  /** Wraps SQL errors from outbox writes so callers can distinguish from upstream JSON failures. */
  public static final class OutboxPublishException extends RuntimeException {
    OutboxPublishException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
