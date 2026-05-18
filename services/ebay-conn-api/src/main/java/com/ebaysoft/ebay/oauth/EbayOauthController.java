package com.ebaysoft.ebay.oauth;

import com.ebaysoft.ebay.config.EbayProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Owns the OAuth 2.0 authorization-code flow with eBay. Phase 1 ships {@code /start} (returns the
 * authorize URL) and {@code /callback} (exchanges the code for tokens and stores them encrypted).
 *
 * <p>Issue #36. Token exchange + persistence land in the next commit; this commit covers the
 * scaffolding + the start endpoint that mints a CSRF state and constructs the eBay URL.
 */
@RestController
@RequestMapping("/v1/oauth/ebay")
@RequiredArgsConstructor
@Slf4j
public class EbayOauthController {

  private static final SecureRandom RNG = new SecureRandom();
  private final EbayProperties props;

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

  /** {@code state} is HMAC-signed in the callback once we wire up the session store. */
  private static String newState() {
    byte[] buf = new byte[24];
    RNG.nextBytes(buf);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
  }

  private static String enc(String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8);
  }

  public record StartResponse(String authorizeUrl, String state) {}
}
