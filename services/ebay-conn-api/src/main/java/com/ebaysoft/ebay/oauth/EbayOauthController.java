package com.ebaysoft.ebay.oauth;

import com.ebaysoft.ebay.config.EbayProperties;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Authorization-code flow with eBay.
 *
 * <ol>
 *   <li>{@code POST /start} — the SPA calls this for an authenticated tenant. We mint a
 *       cryptographically random {@code state}, persist {@code (state, tenantId, marketplaceId,
 *       expiresAt)}, and hand back the authorize URL the user should be redirected to.
 *   <li>{@code GET /callback} — eBay redirects the seller here with {@code ?code=&state=}. We look
 *       up the state, exchange the code for tokens via {@link EbayTokenClient}, persist the
 *       (encrypted) account, and redirect the user to the dashboard.
 * </ol>
 *
 * <p>State CSRF protection: the row is consumed (deleted) on callback, single-use. Expired states
 * are rejected on validation.
 */
@RestController
@RequestMapping("/v1/oauth/ebay")
@RequiredArgsConstructor
@Slf4j
public class EbayOauthController {

  /** State value is 32 random bytes, base64url-encoded (~43 chars). */
  private static final SecureRandom RNG = new SecureRandom();

  /** Each state is valid for 10 minutes — plenty for the user to complete the eBay form. */
  static final Duration STATE_TTL = Duration.ofMinutes(10);

  private final EbayProperties props;
  private final EbayConnectionService connectionService;
  private final OauthStateRepository states;

  @PostMapping("/start")
  public Mono<StartResponse> start(
      @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantId,
      @RequestParam(value = "marketplaceId", defaultValue = "EBAY_US") String marketplaceId) {

    if (tenantId == null) {
      return Mono.error(new MissingTenantException());
    }

    String state = newState();
    Instant expires = Instant.now().plus(STATE_TTL);
    OauthState row = OauthState.minted(state, tenantId, marketplaceId, props.redirectUri(), expires);

    return states
        .save(row)
        .thenReturn(new StartResponse(buildAuthorizeUrl(state), state));
  }

  /** Browser-initiated GET fallback (e.g., a plain anchor tag) — same logic as the POST. */
  @GetMapping("/start")
  public Mono<StartResponse> startGet(
      @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantId,
      @RequestParam(value = "marketplaceId", defaultValue = "EBAY_US") String marketplaceId) {
    return start(tenantId, marketplaceId);
  }

  /**
   * eBay redirects the seller here. We respond with a 302 to {@code /dashboard?ebay=connected}
   * (or {@code ?ebay=error} on failure) so the SPA can render the right state without polling.
   */
  @GetMapping("/callback")
  public Mono<ResponseEntity<Void>> callback(
      @RequestParam String code, @RequestParam String state) {
    log.atInfo().addKeyValue("state", state).log("eBay callback received");

    return states
        .findById(state)
        .switchIfEmpty(Mono.error(new InvalidStateException("state not found")))
        .flatMap(row -> {
          if (Instant.now().isAfter(row.expiresAt())) {
            return states.deleteById(state).then(Mono.error(new InvalidStateException("state expired")));
          }
          return states
              .deleteById(state)  // single-use — consume before exchanging the code
              .then(connectionService.complete(code, row.tenantId(), row.marketplaceId(), null));
        })
        .map(account -> redirectTo("/dashboard?ebay=connected"))
        .onErrorResume(e -> {
          log.atWarn().setCause(e).log("eBay callback failed");
          return Mono.just(redirectTo("/dashboard?ebay=error"));
        });
  }

  /* ----- helpers ----- */

  private String buildAuthorizeUrl(String state) {
    return props.authorizeUrl()
        + "?client_id=" + enc(props.clientId())
        + "&response_type=code"
        + "&redirect_uri=" + enc(props.redirectUri())
        + "&scope=" + enc(props.scopes())
        + "&state=" + state;
  }

  private static String newState() {
    byte[] buf = new byte[32];
    RNG.nextBytes(buf);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
  }

  private static String enc(String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8);
  }

  private static ResponseEntity<Void> redirectTo(String location) {
    return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, location).build();
  }

  public record StartResponse(String authorizeUrl, String state) {}

  static class InvalidStateException extends RuntimeException {
    InvalidStateException(String message) {
      super(message);
    }
  }

  static class MissingTenantException extends RuntimeException {
    MissingTenantException() {
      super("X-Tenant-Id header is required");
    }
  }
}
