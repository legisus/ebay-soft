package com.ebaysoft.domain.ids;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SkuCodeTest {

  @Test
  void of_accepts_alphanumeric_with_dashes_and_underscores_normalized_to_upper() {
    assertThat(SkuCode.of("ABC-123_v2").value()).isEqualTo("ABC-123_V2");
  }

  @Test
  void of_trims_surrounding_whitespace_and_uppercases() {
    assertThat(SkuCode.of("  abc-123  ").value()).isEqualTo("ABC-123");
  }

  @Test
  void rejects_empty_or_whitespace_only_value() {
    assertThatThrownBy(() -> SkuCode.of("")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> SkuCode.of("   ")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejects_longer_than_64_chars() {
    String s = "A".repeat(65);
    assertThatThrownBy(() -> SkuCode.of(s)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejects_disallowed_characters() {
    assertThatThrownBy(() -> SkuCode.of("has space")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> SkuCode.of("slash/in/it")).isInstanceOf(IllegalArgumentException.class);
  }
}
