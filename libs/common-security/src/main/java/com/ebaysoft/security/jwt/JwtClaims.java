package com.ebaysoft.security.jwt;

import java.time.Instant;

/**
 * Verified JWT claims a downstream service can trust. Only the fields we propagate today. Custom
 * claims (tenant, plan, etc.) will be added behind a wider claim API once auth-api needs them.
 */
public record JwtClaims(
    String subject, String audience, String issuer, Instant issuedAt, Instant expiresAt) {}
