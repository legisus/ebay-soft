package com.ebaysoft.ebay.accounts;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EbayAccountRepository extends ReactiveCrudRepository<EbayAccount, UUID> {

  Mono<EbayAccount> findByTenantIdAndEbayUserId(UUID tenantId, String ebayUserId);

  /** Read side for {@code GET /v1/ebay/accounts}. Ordered newest-first for the dashboard. */
  Flux<EbayAccount> findAllByTenantIdOrderByConnectedAtDesc(UUID tenantId);
}
