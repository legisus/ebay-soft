package com.ebaysoft.ebay.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Shape of {@code POST /identity/v1/oauth2/token} on eBay — just the fields we use.
 * https://developer.ebay.com/api-docs/static/oauth-authcode-grant-request.html
 */
public record EbayTokenResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("expires_in") long expiresIn,
    @JsonProperty("refresh_token_expires_in") Long refreshTokenExpiresIn,
    @JsonProperty("token_type") String tokenType) {}
