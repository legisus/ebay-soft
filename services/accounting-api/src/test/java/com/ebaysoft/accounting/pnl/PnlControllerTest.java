package com.ebaysoft.accounting.pnl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ebaysoft.domain.money.Money;
import com.ebaysoft.web.money.MoneyJacksonModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PnlControllerTest {

  private static final UUID TENANT = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private final PnlRepository repo = Mockito.mock(PnlRepository.class);
  private final PnlController controller = new PnlController(repo);

  @Test
  void returns_pnl_rows_from_repo() {
    Pnl row = new Pnl(
        TENANT, LocalDate.of(2026, 5, 1), "day", "2026-05-01",
        Money.of("100.00", "USD"), Money.of("10.00", "USD"), Money.of("0.00", "USD"),
        Money.of("40.00", "USD"), Money.of("5.00", "USD"), Money.of("0.00", "USD"),
        Money.of("45.00", "USD"));
    when(repo.find(any())).thenReturn(List.of(row));

    List<Pnl> result = controller.pnl(
        TENANT, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), "day");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).net()).isEqualTo(Money.of("45.00", "USD"));
  }

  @Test
  void money_serializes_as_amount_string_plus_currency() throws Exception {
    Pnl row = new Pnl(
        TENANT, LocalDate.of(2026, 5, 1), "day", "2026-05-01",
        Money.of("100.00", "USD"), Money.of("10.00", "USD"), Money.of("0.00", "USD"),
        Money.of("40.00", "USD"), Money.of("5.00", "USD"), Money.of("0.00", "USD"),
        Money.of("45.00", "USD"));

    ObjectMapper mapper = new ObjectMapper()
        .registerModule(new MoneyJacksonModule())
        .registerModule(new JavaTimeModule());
    String json = mapper.writeValueAsString(row);

    assertThat(json)
        .contains("\"amount\":\"100.00\"")
        .contains("\"currency\":\"USD\"")
        .contains("\"net\":{\"amount\":\"45.00\"");
  }
}
