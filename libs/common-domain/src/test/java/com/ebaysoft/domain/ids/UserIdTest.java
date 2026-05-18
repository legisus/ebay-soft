package com.ebaysoft.domain.ids;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserIdTest {

  @Test
  void wraps_a_uuid_and_exposes_it() {
    UUID uuid = UUID.randomUUID();
    assertThat(new UserId(uuid).value()).isEqualTo(uuid);
  }

  @Test
  void random_generates_v4_uuid() {
    assertThat(UserId.random().value().version()).isEqualTo(4);
  }

  @Test
  void rejects_null_uuid() {
    assertThatThrownBy(() -> new UserId(null)).isInstanceOf(NullPointerException.class);
  }
}
