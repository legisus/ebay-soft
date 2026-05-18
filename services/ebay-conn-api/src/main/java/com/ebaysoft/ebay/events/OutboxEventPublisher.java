package com.ebaysoft.ebay.events;

import com.ebaysoft.events.cloudevent.CloudEvent;
import com.ebaysoft.events.publisher.EventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Statement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * R2DBC-backed implementation of {@link EventPublisher}: writes one row to the {@code outbox} table
 * inside the current transaction. A separate forwarder process (lands in a follow-up commit) picks
 * up rows where {@code forwarded_at IS NULL} and emits {@code NOTIFY} on the relevant channel.
 *
 * <p>Only registered when an R2DBC {@code ConnectionFactory} is on the context (i.e. in real
 * application boots, not in lightweight smoke tests that excluded R2DBC autoconfig).
 */
@Component
@Primary
@ConditionalOnBean(ConnectionFactory.class)
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher implements EventPublisher {

  private final ConnectionFactory connectionFactory;
  private final ObjectMapper objectMapper;

  @Override
  public void publish(CloudEvent event) {
    // Fire-and-forget at the public API boundary — Spring's reactive tx manager joins this Mono
    // into the calling reactive transaction via subscribe context.
    Mono.from(connectionFactory.create())
        .flatMapMany(conn -> {
          String json;
          try {
            json = objectMapper.writeValueAsString(event.data());
          } catch (JsonProcessingException e) {
            return Mono.error(new IllegalStateException("could not serialize event data", e));
          }
          Statement st =
              conn.createStatement(
                      "INSERT INTO outbox(event_id, type, source, subject, time, payload) "
                          + "VALUES ($1, $2, $3, $4, $5, $6::jsonb)")
                  .bind("$1", event.id())
                  .bind("$2", event.type())
                  .bind("$3", event.source())
                  .bind("$5", event.time())
                  .bind("$6", json);
          if (event.subject() != null) {
            st.bind("$4", event.subject());
          } else {
            st.bindNull("$4", String.class);
          }
          return Mono.from(st.execute())
              .doFinally(s -> Mono.from(conn.close()).subscribe());
        })
        .doOnError(e -> log.atError().setCause(e).log("failed to write event to outbox"))
        .subscribe();
  }
}
