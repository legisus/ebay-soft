package com.ebaysoft.events.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ebaysoft.events.cloudevent.CloudEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

class OutboxEventPublisherTest {

  /**
   * Default path: Testcontainers spins up Postgres. Override path: set {@code TEST_PG_URL}
   * (e.g., for laptops without Docker) and the test will hit that DB instead. CI uses the
   * default path; the override is for local-only verification.
   */
  private static PostgreSQLContainer<?> container;

  private static Connection conn;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @BeforeAll
  static void setup() throws Exception {
    String overrideUrl = System.getenv("TEST_PG_URL");
    String url;
    String user;
    String pass;
    if (overrideUrl != null && !overrideUrl.isBlank()) {
      url = overrideUrl;
      user = System.getenv().getOrDefault("TEST_PG_USER", System.getProperty("user.name"));
      pass = System.getenv().getOrDefault("TEST_PG_PASS", "");
    } else {
      container = new PostgreSQLContainer<>("postgres:16-alpine");
      container.start();
      url = container.getJdbcUrl();
      user = container.getUsername();
      pass = container.getPassword();
    }
    conn = DriverManager.getConnection(url, user, pass);
    try (Statement s = conn.createStatement()) {
      s.execute("DROP TABLE IF EXISTS outbox");
      s.execute(OutboxDdl.CREATE_OUTBOX);
    }
  }

  @AfterAll
  static void tearDown() throws Exception {
    if (conn != null) {
      try (Statement s = conn.createStatement()) {
        s.execute("DROP TABLE IF EXISTS outbox");
      } catch (Exception ignored) {
        // best-effort cleanup
      }
      conn.close();
    }
    if (container != null) {
      container.stop();
    }
  }

  @BeforeEach
  void truncate() throws Exception {
    try (Statement s = conn.createStatement()) {
      s.execute("TRUNCATE outbox");
    }
  }

  @Test
  void writes_event_row_with_all_fields_serialized() throws Exception {
    CloudEvent event =
        CloudEvent.builder()
            .id("evt-1")
            .type("ebay-soft.order.synced.v1")
            .source("/sync-api")
            .subject("tenant-abc")
            .time(Instant.parse("2026-05-18T10:00:00Z"))
            .data(Map.of("orderId", "42", "amount", "19.99"))
            .build();

    new OutboxEventPublisher(() -> conn, MAPPER).publish(event);

    try (Statement s = conn.createStatement();
        ResultSet rs =
            s.executeQuery(
                "SELECT event_id, type, source, subject, time, payload, forwarded_at FROM outbox")) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getString("event_id")).isEqualTo("evt-1");
      assertThat(rs.getString("type")).isEqualTo("ebay-soft.order.synced.v1");
      assertThat(rs.getString("source")).isEqualTo("/sync-api");
      assertThat(rs.getString("subject")).isEqualTo("tenant-abc");
      assertThat(rs.getTimestamp("time").toInstant())
          .isEqualTo(Instant.parse("2026-05-18T10:00:00Z"));
      // Postgres JSONB normalizes whitespace; parse to compare semantically.
      var payload = MAPPER.readTree(rs.getString("payload"));
      assertThat(payload.get("orderId").asText()).isEqualTo("42");
      assertThat(payload.get("amount").asText()).isEqualTo("19.99");
      assertThat(rs.getTimestamp("forwarded_at")).isNull();
      assertThat(rs.next()).isFalse();
    }
  }

  @Test
  void duplicate_event_id_violates_unique_constraint() {
    OutboxEventPublisher publisher = new OutboxEventPublisher(() -> conn, MAPPER);
    CloudEvent event =
        CloudEvent.builder()
            .id("evt-dup")
            .type("t")
            .source("/s")
            .data(Map.of("k", "v"))
            .build();
    publisher.publish(event);

    assertThatThrownBy(() -> publisher.publish(event))
        .isInstanceOf(OutboxEventPublisher.OutboxPublishException.class)
        .hasMessageContaining("evt-dup");
  }
}
