package com.ebaysoft.ebay.marketplacedeletion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the eBay Marketplace Account Deletion / Closure endpoint.
 *
 * <p>Per eBay's spec the {@code verificationToken} is a 32–80 char value set in the developer
 * portal; we read it from a sealed env var. {@code endpointUrl} is the public URL eBay calls and
 * must EXACTLY match what's registered in the portal — both are inputs to the SHA-256 hash.
 */
@ConfigurationProperties(prefix = "ebay-soft.ebay.marketplace-deletion")
@Validated
public record MarketplaceDeletionProperties(
    @NotBlank @Size(min = 32, max = 80) String verificationToken,
    @NotBlank String endpointUrl) {}
