package com.ebaysoft.sync.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EbayNotificationControllerTest {

  @Test
  void item_sold_maps_to_order_synced() {
    assertThat(EbayNotificationController.cloudEventSuffix("ITEM_SOLD"))
        .isEqualTo("order.synced.v1");
  }

  @Test
  void item_changed_maps_to_listing_synced() {
    assertThat(EbayNotificationController.cloudEventSuffix("ITEM_CHANGED"))
        .isEqualTo("listing.synced.v1");
  }

  @Test
  void payout_maps_to_finance_event() {
    assertThat(EbayNotificationController.cloudEventSuffix("PAYOUT_PAID"))
        .isEqualTo("finance_event.recorded.v1");
  }

  @Test
  void unknown_topic_routes_to_raw_namespace_for_triage() {
    assertThat(EbayNotificationController.cloudEventSuffix("ACCOUNT_DELETION"))
        .isEqualTo("raw.account.deletion.v1");
    assertThat(EbayNotificationController.cloudEventSuffix(null))
        .isEqualTo("raw.unknown.v1");
  }
}
