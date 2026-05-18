package com.ebaysoft.domain.ids;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantIdTest {

  @Test
  void wraps_a_uuid_and_exposes_it() {
    UUID uuid = UUID.randomUUID();
    TenantId id = new TenantId(uuid);
    assertThat(id.value()).isEqualTo(uuid);
  }

  @Test
  void random_generates_a_unique_v4_uuid() {
    TenantId a = TenantId.random();
    TenantId b = TenantId.random();
    assertThat(a).isNotEqualTo(b);
    assertThat(a.value().version()).isEqualTo(4);
  }

  @Test
  void fromString_parses_uuid_text() {
    UUID uuid = UUID.randomUUID();
    assertThat(TenantId.fromString(uuid.toString())).isEqualTo(new TenantId(uuid));
  }

  @Test
  void rejects_null_uuid() {
    assertThatThrownBy(() -> new TenantId(null)).isInstanceOf(NullPointerException.class);
  }
}
