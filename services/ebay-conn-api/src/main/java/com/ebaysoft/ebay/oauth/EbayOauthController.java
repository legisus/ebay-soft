package com.ebaysoft.ebay.oauth;

import com.ebaysoft.ebay.config.EbayProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Owns the OAuth 2.0 authorization-code flow with eBay: {@code /start} returns the authorize URL,
 * {@code /callback} exchanges the code for tokens, encrypts the refresh token, persists the
 * account, and publishes {@code ebay_account.connected} via the outbox.
 */
@RestController
@RequestMapping("/v1/oauth/ebay")
@RequiredArgsConstructor
@Slf4j
public class EbayOauthController {

  private static final SecureRandom RNG = new SecureRandom();
  private final EbayProperties props;
  private final EbayConnectionService connectionService;

  @GetMapping("/start")
  public Mono<StartResponse> start() {
    String state = newState();
    String url =
        props.authorizeUrl()
            + "?client_id=" + enc(props.clientId())
            + "&response_type=code"
            + "&redirect_uri=" + enc(props.redirectUri())
            + "&scope=" + enc(props.scopes())
            + "&state=" + state;
    log.atInfo().addKeyValue("state", state).log("issuing eBay authorize URL");
    return Mono.just(new StartResponse(url, state));
  }

  /**
   * eBay redirects the seller here with {@code ?code=...&state=...}. For Phase 1 we accept
   * tenantId/marketplaceId/ebayUserId as query params so the flow is testable; once the SPA flow
   * lands, state is HMAC-signed and carries tenantId, and the user-info comes from eBay's
   * {@code /commerce/identity/v1/user} call.
   */
  @GetMapping("/callback")
  public Mono<CallbackResponse> callback(
      @RequestParam String code,
      @RequestParam String state,
      @RequestParam("tenantId") UUID tenantId,
      @RequestParam("marketplaceId") String marketplaceId,
      @RequestParam("ebayUserId") String ebayUserId) {
    log.atInfo().addKeyValue("state", state).log("eBay callback received");
    return connectionService
        .complete(code, tenantId, marketplaceId, ebayUserId)
        .map(account -> new CallbackResponse(account.id().toString(), "connected"));
  }

  private static String newState() {
    byte[] buf = new byte[24];
    RNG.nextBytes(buf);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
  }

  private static String enc(String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8);
  }

  public record StartResponse(String authorizeUrl, String state) {}

  public record CallbackResponse(String ebayAccountId, String status) {}
}
