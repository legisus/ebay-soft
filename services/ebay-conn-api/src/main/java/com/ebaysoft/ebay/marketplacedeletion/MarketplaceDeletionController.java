package com.ebaysoft.ebay.marketplacedeletion;

import com.ebaysoft.events.cloudevent.CloudEvent;
import com.ebaysoft.events.publisher.EventPublisher;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * eBay-mandatory Marketplace Account Deletion / Closure endpoint. eBay verifies our URL with a
 * GET handshake before enabling deletion notifications; on real delete events we MUST acknowledge
 * with 200 and hard-delete the affected seller's PII.
 *
 * <p>Protocol: {@code sha256(challenge_code + verification_token + endpoint_url)} returned as
 * hex.  See https://developer.ebay.com/marketplace-account-deletion
 *
 * <p>Issue #39.
 */
@RestController
@RequestMapping("/v1/ebay/marketplace-deletion")
@RequiredArgsConstructor
@Slf4j
public class MarketplaceDeletionController {

  private final MarketplaceDeletionProperties props;
  private final EventPublisher events;

  @GetMapping
  public Mono<Map<String, String>> challenge(@RequestParam("challenge_code") String challengeCode) {
    log.atInfo().addKeyValue("challengeCode", challengeCode).log("eBay deletion endpoint challenge");
    String response = sha256Hex(challengeCode + props.verificationToken() + props.endpointUrl());
    return Mono.just(Map.of("challengeResponse", response));
  }

  /**
   * Real deletion notification. We always 200 (eBay retries forever otherwise) and emit a
   * {@code tenant.gdpr_purge} event that other services consume to wipe per-tenant rows.
   */
  @PostMapping
  public Mono<Void> notify(@RequestBody Map<String, Object> body) {
    log.atInfo()
        .addKeyValue("notification", body)
        .log("eBay deletion notification received");
    events.publish(
        CloudEvent.builder()
            .type("ebay-soft.tenant.gdpr_purge.v1")
            .source("/ebay-conn-api")
            .subject(extractEbayUserId(body))
            .data(body)
            .build());
    return Mono.empty();
  }

  private static String extractEbayUserId(Map<String, Object> body) {
    Object notification = body.get("notification");
    if (notification instanceof Map<?, ?> n) {
      Object data = n.get("data");
      if (data instanceof Map<?, ?> d) {
        Object userId = d.get("username");
        if (userId != null) return userId.toString();
      }
    }
    return "unknown";
  }

  private static String sha256Hex(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
