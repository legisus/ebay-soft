package com.ebaysoft.ebay.oauth;

import com.ebaysoft.ebay.accounts.EbayAccount;
import com.ebaysoft.ebay.accounts.EbayAccountRepository;
import com.ebaysoft.events.cloudevent.CloudEvent;
import com.ebaysoft.events.publisher.EventPublisher;
import com.ebaysoft.security.crypto.TokenCipher;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Periodically refreshes eBay access tokens that are within {@link #REFRESH_HORIZON} of expiry.
 * eBay's access tokens last 2 hours; we refresh anything expiring in the next 15 minutes so a
 * short outage of this worker can't strand any tenant.
 *
 * <p>On refresh failure the account is marked {@code expired} and an {@code ebay_account.expired.v1}
 * event is emitted; {@code notif-api} consumes the event and emails the seller.
 *
 * <p>Issue #38. Disabled by default in unit tests via {@code ebay-soft.ebay.token-refresh.enabled};
 * production turns it on.
 */
@Component
@ConditionalOnProperty(
    prefix = "ebay-soft.ebay.token-refresh",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class EbayRefreshTokenWorker {

  static final Duration REFRESH_HORIZON = Duration.ofMinutes(15);

  private final EbayAccountRepository accounts;
  private final EbayTokenClient tokenClient;
  private final TokenCipher cipher;
  private final EventPublisher events;

  /** Runs every 5 minutes. Concurrency-safe — Spring's scheduler serializes per-bean by default. */
  @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT30S")
  public void refreshExpiring() {
    Instant cutoff = Instant.now().plus(REFRESH_HORIZON);
    accounts.findAll()
        .filter(acc -> "connected".equals(acc.status()))
        .filter(acc -> acc.accessTokenExpiresAt() != null
            && acc.accessTokenExpiresAt().isBefore(cutoff))
        .flatMap(this::refreshOne, /* concurrency */ 4)
        .subscribe(
            saved -> log.atInfo()
                .addKeyValue("ebayAccountId", saved.id())
                .addKeyValue("expiresAt", saved.accessTokenExpiresAt())
                .log("refreshed eBay access token"),
            err -> log.atError().setCause(err).log("refresh sweep failed"));
  }

  private Mono<EbayAccount> refreshOne(EbayAccount account) {
    String refreshToken = cipher.decryptToString(account.refreshTokenEnc());
    return tokenClient
        .refreshAccessToken(refreshToken)
        .flatMap(tokens -> {
          EbayAccount updated = new EbayAccount(
              account.id(),
              account.tenantId(),
              account.marketplaceId(),
              account.ebayUserId(),
              account.refreshTokenEnc(),
              Instant.now().plusSeconds(tokens.expiresIn()),
              "connected",
              account.connectedAt());
          return accounts.save(updated);
        })
        .onErrorResume(err -> markExpired(account, err));
  }

  private Mono<EbayAccount> markExpired(EbayAccount account, Throwable cause) {
    log.atWarn()
        .addKeyValue("ebayAccountId", account.id())
        .addKeyValue("tenantId", account.tenantId())
        .setCause(cause)
        .log("eBay account refresh failed — marking expired");

    EbayAccount expired = new EbayAccount(
        account.id(), account.tenantId(), account.marketplaceId(), account.ebayUserId(),
        account.refreshTokenEnc(), account.accessTokenExpiresAt(), "expired", account.connectedAt());

    events.publish(CloudEvent.builder()
        .type("ebay-soft.ebay_account.expired.v1")
        .source("/ebay-conn-api")
        .subject(account.tenantId().toString())
        .data(Map.of(
            "ebayAccountId", account.id().toString(),
            "ebayUserId", account.ebayUserId(),
            "reason", cause.getMessage()))
        .build());

    return accounts.save(expired);
  }
}
