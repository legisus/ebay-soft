package com.ebaysoft.accounting.pnl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PnlQueryTest {

  private static final UUID TENANT = UUID.randomUUID();

  @Test
  void rejects_unknown_groupBy() {
    assertThatThrownBy(() -> new PnlQuery(TENANT, LocalDate.now(), LocalDate.now(), "week"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("groupBy");
  }

  @Test
  void rejects_from_after_to() {
    assertThatThrownBy(() -> new PnlQuery(
            TENANT, LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 1), "day"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("from");
  }

  @Test
  void rejects_null_tenant() {
    assertThatThrownBy(() -> new PnlQuery(null, LocalDate.now(), LocalDate.now(), "day"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejects_null_dates() {
    assertThatThrownBy(() -> new PnlQuery(TENANT, null, LocalDate.now(), "day"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new PnlQuery(TENANT, LocalDate.now(), null, "day"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
