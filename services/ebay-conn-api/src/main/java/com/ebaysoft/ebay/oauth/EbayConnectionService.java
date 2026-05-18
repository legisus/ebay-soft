package com.ebaysoft.ebay.oauth;

import com.ebaysoft.ebay.accounts.EbayAccount;
import com.ebaysoft.ebay.accounts.EbayAccountRepository;
import com.ebaysoft.events.cloudevent.CloudEvent;
import com.ebaysoft.events.publisher.EventPublisher;
import com.ebaysoft.security.crypto.TokenCipher;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/**
 * Orchestrates the post-authorize OAuth flow: exchange code → encrypt refresh token → persist
 * connected account → publish {@code ebay_account.connected} event via the outbox.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EbayConnectionService {

  private final EbayTokenClient tokenClient;
  private final EbayAccountRepository accounts;
  private final TokenCipher cipher;
  private final EventPublisher events;

  /**
   * Marketplace + eBay user id arrive separately because the token response doesn't carry them —
   * the front-end calls /v1/oauth/ebay/start with marketplaceId, persists state→marketplaceId, and
   * the callback round-trips both.
   */
  @Transactional
  public Mono<EbayAccount> complete(
      String code, UUID tenantId, String marketplaceId, String ebayUserId) {
    return tokenClient
        .exchangeCode(code)
        .flatMap(tokens -> persistAndAnnounce(tokens, tenantId, marketplaceId, ebayUserId))
        .doOnSuccess(a -> log.atInfo()
            .addKeyValue("tenantId", a.tenantId())
            .addKeyValue("ebayUserId", a.ebayUserId())
            .log("eBay account connected"));
  }

  private Mono<EbayAccount> persistAndAnnounce(
      EbayTokenResponse tokens, UUID tenantId, String marketplaceId, String ebayUserId) {
    byte[] encryptedRefresh = cipher.encrypt(tokens.refreshToken());
    Instant accessExpiresAt = Instant.now().plusSeconds(tokens.expiresIn());

    EbayAccount account =
        EbayAccount.newlyConnected(tenantId, marketplaceId, ebayUserId, encryptedRefresh, accessExpiresAt);

    return accounts
        .save(account)
        .doOnNext(saved -> events.publish(
            CloudEvent.builder()
                .type("ebay-soft.ebay_account.connected.v1")
                .source("/ebay-conn-api")
                .subject(tenantId.toString())
                .data(Map.of(
                    "ebayAccountId", saved.id().toString(),
                    "marketplaceId", marketplaceId,
                    "ebayUserId", ebayUserId))
                .build()));
  }
}
