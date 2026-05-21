package com.ebaysoft.accounting.pnl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ebaysoft.accounting.pnl.OrderSyncedHandler.OrderSyncedPayload;
import com.ebaysoft.domain.money.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Unit test for the mapping logic — no DB. Verifies the handler builds a {@link Pnl} row whose
 * {@code net} matches {@code revenue − fees − refunds − cogs − shipping − ads} and that
 * persistence is keyed by the event id (idempotency lever).
 */
class OrderSyncedHandlerTest {

  private static final Currency USD = Currency.getInstance("USD");
  private static final UUID TENANT = UUID.fromString("11111111-1111-1111-1111-111111111111");

  @Test
  void maps_payload_to_pnl_row_and_calls_idempotent_upsert() {
    PnlRepository repo = Mockito.mock(PnlRepository.class);
    when(repo.upsertFromEvent(any(), any())).thenReturn(1);
    OrderSyncedHandler handler = new OrderSyncedHandler(repo);

    OrderSyncedPayload payload =
        new OrderSyncedPayload(
            TENANT,
            LocalDate.of(2026, 5, 19),
            money("19.99"),
            money("1.50"),
            money("0"),
            money("3.99"),
            money("0"),
            "EBAY_US",
            "order-abc");

    handler.handle(payload, "evt-42");

    ArgumentCaptor<Pnl> rowCaptor = ArgumentCaptor.forClass(Pnl.class);
    verify(repo).upsertFromEvent(rowCaptor.capture(), eq("evt-42"));
    Pnl row = rowCaptor.getValue();

    assertThat(row.tenantId()).isEqualTo(TENANT);
    assertThat(row.date()).isEqualTo(LocalDate.of(2026, 5, 19));
    assertThat(row.groupBy()).isEqualTo("day");
    assertThat(row.groupKey()).isEqualTo("2026-05-19");
    assertThat(row.revenue().amount()).isEqualByComparingTo("19.99");
    assertThat(row.fees().amount()).isEqualByComparingTo("1.50");
    // net = 19.99 − 1.50 − 0 − 0(cogs) − 3.99 − 0 = 14.50
    assertThat(row.net().amount()).isEqualByComparingTo("14.50");
  }

  @Test
  void zero_cogs_today_until_lookup_lands() {
    PnlRepository repo = Mockito.mock(PnlRepository.class);
    when(repo.upsertFromEvent(any(), any())).thenReturn(1);
    OrderSyncedHandler handler = new OrderSyncedHandler(repo);

    handler.handle(
        new OrderSyncedPayload(
            TENANT, LocalDate.now(), money("10"), money("1"), money("0"),
            money("0"), money("0"), "EBAY_US", "x"),
        "evt-cogs-test");

    ArgumentCaptor<Pnl> rowCaptor = ArgumentCaptor.forClass(Pnl.class);
    verify(repo).upsertFromEvent(rowCaptor.capture(), any());
    assertThat(rowCaptor.getValue().cogs().amount()).isEqualByComparingTo("0");
  }

  private static Money money(String amount) {
    return new Money(new BigDecimal(amount), USD);
  }
}
