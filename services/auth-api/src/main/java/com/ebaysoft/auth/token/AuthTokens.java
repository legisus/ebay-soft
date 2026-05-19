package com.ebaysoft.auth.token;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Issues and verifies the two token kinds auth-api hands out:
 *
 * <ul>
 *   <li><b>Access</b> — short-lived (15 min) HS256 JWT carrying {@code sub=userId}, {@code
 *       tenantId} claim, and {@code email}. Stateless: any service holding the secret can verify.
 *   <li><b>Refresh</b> — opaque 256-bit random string (NOT a JWT). The raw value is returned to
 *       the client once; we persist only the SHA-256 hash so a DB compromise can't resurrect a
 *       working credential. Lookup happens in {@link RefreshTokens}.
 * </ul>
 *
 * <p>HS256 + a single shared secret is appropriate for a single-deploy MVP. Once the system grows
 * beyond Hetzner (or accepts external token consumers), this class gets an RS256 sibling — see
 * {@code docs/SECURITY.md} → JWT signing keys.
 */
@Component
public class AuthTokens {

  /** Audience claim distinguishes user-issued tokens from {@code ServiceAccountJwt} ones. */
  public static final String USER_AUDIENCE = "ebay-soft.user";

  public static final Duration ACCESS_TTL = Duration.ofMinutes(15);
  public static final Duration REFRESH_TTL = Duration.ofDays(30);

  private final byte[] secret;
  private final String issuer;
  private final SecureRandom random = new SecureRandom();

  public AuthTokens(
      @Value("${ebay-soft.auth.service-account-secret}") String secret,
      @Value("${ebay-soft.auth.jwt-issuer}") String issuer) {
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
    this.issuer = issuer;
  }

  /** Issue an HS256 access token for the given user. */
  public String issueAccess(UUID userId, UUID tenantId, String email) {
    Instant now = Instant.now();
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject(userId.toString())
            .audience(USER_AUDIENCE)
            .claim("tenantId", tenantId.toString())
            .claim("email", email)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plus(ACCESS_TTL)))
            .build();
    try {
      SignedJWT signed = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
      signed.sign(new MACSigner(secret));
      return signed.serialize();
    } catch (JOSEException e) {
      throw new IllegalStateException("failed to sign access token", e);
    }
  }

  /**
   * Verify an access token and return the embedded claims. Throws {@link InvalidAccessTokenException}
   * for any reason the token isn't trustworthy.
   */
  public AccessClaims verifyAccess(String token) {
    try {
      SignedJWT parsed = SignedJWT.parse(token);
      if (!parsed.verify(new MACVerifier(secret))) {
        throw new InvalidAccessTokenException("signature verification failed");
      }
      JWTClaimsSet claims = parsed.getJWTClaimsSet();
      Date exp = claims.getExpirationTime();
      if (exp == null || exp.toInstant().isBefore(Instant.now())) {
        throw new InvalidAccessTokenException("token expired");
      }
      String tenant = claims.getStringClaim("tenantId");
      String email = claims.getStringClaim("email");
      if (claims.getSubject() == null || tenant == null) {
        throw new InvalidAccessTokenException("missing required claims");
      }
      return new AccessClaims(UUID.fromString(claims.getSubject()), UUID.fromString(tenant), email);
    } catch (ParseException | JOSEException e) {
      throw new InvalidAccessTokenException("could not verify access token: " + e.getMessage(), e);
    }
  }

  /** Generate a fresh opaque refresh token (base64url-encoded 32 random bytes). */
  public String newRefresh() {
    byte[] buf = new byte[32];
    random.nextBytes(buf);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
  }

  public record AccessClaims(UUID userId, UUID tenantId, String email) {}

  public static class InvalidAccessTokenException extends RuntimeException {
    public InvalidAccessTokenException(String message) {
      super(message);
    }

    public InvalidAccessTokenException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
