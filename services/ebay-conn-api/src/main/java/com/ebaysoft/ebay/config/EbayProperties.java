package com.ebaysoft.ebay.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * eBay developer-program credentials. Pulled from sealed env vars at boot — see
 * docs/SECURITY.md → Secrets. Each environment (sandbox / production) gets its own pair;
 * production reads {@code EBAY_PROD_*}, staging reads {@code EBAY_SANDBOX_*}.
 */
@ConfigurationProperties(prefix = "ebay-soft.ebay")
@Validated
public record EbayProperties(
    @NotBlank String clientId,
    @NotBlank String clientSecret,
    @NotBlank String redirectUri,
    @NotNull URI authorizeUrl,
    @NotNull URI tokenUrl,
    @NotBlank String scopes) {}
