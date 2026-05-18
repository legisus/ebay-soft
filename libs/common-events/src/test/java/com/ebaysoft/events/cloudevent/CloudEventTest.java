package com.ebaysoft.events.cloudevent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CloudEventTest {

  @Test
  void builder_populates_required_fields_and_defaults() {
    Instant before = Instant.now();
    CloudEvent event =
        CloudEvent.builder()
            .type("ebay-soft.order.synced.v1")
            .source("/sync-api")
            .subject("tenant-abc")
            .data(Map.of("orderId", "42"))
            .build();

    assertThat(event.id()).isNotBlank();
    assertThat(event.specversion()).isEqualTo("1.0");
    assertThat(event.type()).isEqualTo("ebay-soft.order.synced.v1");
    assertThat(event.source()).isEqualTo("/sync-api");
    assertThat(event.subject()).isEqualTo("tenant-abc");
    assertThat(event.time()).isBetween(before.minusSeconds(1), Instant.now().plusSeconds(1));
    assertThat(event.datacontenttype()).isEqualTo("application/json");
    assertThat(event.data()).isEqualTo(Map.of("orderId", "42"));
  }

  @Test
  void explicit_id_and_time_are_honored() {
    Instant fixed = Instant.parse("2026-01-01T00:00:00Z");
    CloudEvent event =
        CloudEvent.builder()
            .id("evt-123")
            .time(fixed)
            .type("t")
            .source("s")
            .data(Map.of())
            .build();
    assertThat(event.id()).isEqualTo("evt-123");
    assertThat(event.time()).isEqualTo(fixed);
  }

  @Test
  void builder_requires_type_and_source() {
    assertThatThrownBy(() -> CloudEvent.builder().source("s").data(Map.of()).build())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("type");
    assertThatThrownBy(() -> CloudEvent.builder().type("t").data(Map.of()).build())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("source");
  }
}
