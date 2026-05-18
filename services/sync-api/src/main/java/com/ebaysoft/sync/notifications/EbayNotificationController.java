package com.ebaysoft.sync.notifications;

import com.ebaysoft.events.cloudevent.CloudEvent;
import com.ebaysoft.events.publisher.EventPublisher;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * eBay Platform Notifications webhook receiver. eBay POSTs JSON envelopes here whenever a
 * subscribed topic fires (item sold, item changed, dispute opened, payout, etc.). We acknowledge
 * with 200 quickly so eBay doesn't retry-storm, then translate the payload into one of our
 * CloudEvents on the outbox for downstream services.
 *
 * <p>Issue #43. Per-topic unpackers (order.synced, listing.synced, finance_event.recorded) land
 * as we wire each consumer; this commit covers the envelope, dedupe-friendly logging, and the
 * generic outbox bridge.
 */
@RestController
@RequestMapping("/v1/sync/notifications/ebay")
@RequiredArgsConstructor
@Slf4j
public class EbayNotificationController {

  private final EventPublisher events;

  @PostMapping
  public Mono<ResponseEntity<Void>> receive(
      @RequestBody EbayNotification notification,
      @RequestHeader(value = "X-EBAY-SIGNATURE", required = false) String signature,
      @RequestHeader(HttpHeaders.USER_AGENT) String userAgent) {

    log.atInfo()
        .addKeyValue("notificationId", notification.notificationId())
        .addKeyValue("topic", notification.topic())
        .addKeyValue("ua", userAgent)
        .addKeyValue("signed", signature != null)
        .log("eBay notification received");

    String cloudEventType = "ebay-soft." + cloudEventSuffix(notification.topic());
    events.publish(CloudEvent.builder()
        .id(notification.notificationId())   // dedupes downstream
        .type(cloudEventType)
        .source("/sync-api")
        .subject(extractSubject(notification.data()))
        .data(notification)
        .build());

    return Mono.just(ResponseEntity.ok().build());
  }

  /**
   * Maps eBay topic names ({@code ITEM_SOLD}, {@code ITEM_CHANGED}, …) onto our event type suffix.
   * Unknown topics are routed under {@code raw.<topic>.v1} for triage.
   */
  static String cloudEventSuffix(String topic) {
    if (topic == null) return "raw.unknown.v1";
    return switch (topic) {
      case "ITEM_SOLD", "ORDER_CREATED" -> "order.synced.v1";
      case "ITEM_CHANGED", "ITEM_LISTED", "ITEM_REVISED" -> "listing.synced.v1";
      case "PAYOUT_PAID", "PAYOUT_RETRIED" -> "finance_event.recorded.v1";
      default -> "raw." + topic.toLowerCase().replace('_', '.') + ".v1";
    };
  }

  private static String extractSubject(Map<String, Object> data) {
    if (data == null) return null;
    Object v = data.get("username");
    if (v == null) v = data.get("userId");
    if (v == null) v = data.get("orderId");
    return v == null ? null : v.toString();
  }
}
