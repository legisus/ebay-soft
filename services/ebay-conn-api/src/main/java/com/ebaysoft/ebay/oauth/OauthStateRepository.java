package com.ebaysoft.ebay.oauth;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface OauthStateRepository extends ReactiveCrudRepository<OauthState, String> {}
