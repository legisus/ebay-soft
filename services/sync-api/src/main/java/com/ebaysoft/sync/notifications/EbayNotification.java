package com.ebaysoft.sync.notifications;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.Map;

/**
 * Shape of an eBay Platform Notification POST body. eBay's notifications are JSON wrappers with a
 * {@code notificationId} for dedupe, a {@code topic} like {@code ITEM_SOLD}, and a
 * {@code data} blob whose shape varies per topic. We capture the dispatch envelope here and treat
 * {@code data} as opaque JSON until the per-topic handler unpacks it.
 *
 * <p>See https://developer.ebay.com/api-docs/static/notifications.html
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EbayNotification(
    @JsonAlias({"notificationId", "notification_id"}) String notificationId,
    @JsonAlias({"eventDate", "event_date"}) Instant eventDate,
    @JsonAlias({"publishDate", "publish_date"}) Instant publishDate,
    String topic,
    Map<String, Object> data) {}
