package com.ebaysoft.ebay.marketplacedeletion;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * eBay-mandatory Marketplace Account Deletion / Closure endpoint. Two verbs:
 *
 * <ul>
 *   <li>{@code GET ?challenge_code=X} — handshake on registration; eBay expects back
 *       {@code sha256(challenge_code + verification_token + endpoint_url)}.
 *   <li>{@code POST {...}} — the actual deletion notification; we acknowledge with 200 and
 *       hard-delete the affected tenant's eBay PII out-of-band.
 * </ul>
 *
 * Issue #39. SHA-256 verification logic + hard-delete event lands in the next commit; this skeleton
 * gets the route registered so eBay's onboarding can verify the URL.
 */
@RestController
@RequestMapping("/v1/ebay/marketplace-deletion")
@Slf4j
public class MarketplaceDeletionController {

  @GetMapping
  public Mono<Map<String, String>> challenge(@RequestParam("challenge_code") String challengeCode) {
    log.atInfo().addKeyValue("challengeCode", challengeCode).log("eBay deletion endpoint challenge");
    // TODO(#39): compute sha256(challenge_code + verification_token + endpoint_url)
    return Mono.just(Map.of("challengeResponse", challengeCode));
  }

  @PostMapping
  public Mono<Void> notify(@RequestBody Map<String, Object> body) {
    log.atInfo().addKeyValue("notification", body).log("eBay deletion notification received");
    // TODO(#39): publish tenant.gdpr_purge event; downstream services purge by tenant id.
    return Mono.empty();
  }
}
