package com.ebaysoft.ebay.accounts;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface EbayAccountRepository extends ReactiveCrudRepository<EbayAccount, UUID> {

  Mono<EbayAccount> findByTenantIdAndEbayUserId(UUID tenantId, String ebayUserId);
}
