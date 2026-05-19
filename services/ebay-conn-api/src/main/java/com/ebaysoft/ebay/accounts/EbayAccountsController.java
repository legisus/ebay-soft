package com.ebaysoft.ebay.accounts;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Read API the SPA dashboard uses to know if a tenant has connected an eBay account.
 *
 * <p><b>No tokens leak through this surface</b> — only the public-facing fields. The refresh-token
 * column stays encrypted at rest and is decrypted only inside {@code ebay-conn-api}'s own
 * outbound calls to eBay.
 */
@RestController
@RequestMapping("/v1/ebay/accounts")
@RequiredArgsConstructor
public class EbayAccountsController {

  private final EbayAccountRepository accounts;

  @GetMapping
  public Mono<AccountListResponse> list(
      @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantId) {
    if (tenantId == null) {
      // Until the gateway JWT filter forwards headers (#162 follow-up), missing tenant ⇒ empty.
      return Mono.just(new AccountListResponse(java.util.List.of()));
    }
    return accounts
        .findAllByTenantIdOrderByConnectedAtDesc(tenantId)
        .map(AccountView::from)
        .collectList()
        .map(AccountListResponse::new);
  }

  public record AccountListResponse(java.util.List<AccountView> accounts) {}

  public record AccountView(
      String id,
      String marketplaceId,
      String ebayUserId,
      String status,
      Instant connectedAt) {

    static AccountView from(EbayAccount a) {
      return new AccountView(
          a.id().toString(),
          a.marketplaceId(),
          a.ebayUserId(),
          a.status(),
          a.connectedAt());
    }
  }
}
