package com.ebaysoft.security.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

/**
 * Symmetric (HS256) service-to-service JWT issuer/verifier. Caller services attach the token to
 * inter-service REST calls; the receiving service rejects requests without a valid one.
 *
 * <p>HS256 is fine for inter-service traffic on a single deploy (Hetzner box). Once we introduce
 * key rotation or external consumers, this class gets a sibling that does RS256 + JWKS — see
 * docs/SECURITY.md.
 */
public final class ServiceAccountJwt {

  private final byte[] secret;
  private final String issuer;

  public ServiceAccountJwt(String secret, String issuer) {
    Objects.requireNonNull(secret, "secret");
    Objects.requireNonNull(issuer, "issuer");
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
    this.issuer = issuer;
  }

  public String issue(String subject, String audience, Duration ttl) {
    Instant now = Instant.now();
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject(subject)
            .audience(audience)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plus(ttl)))
            .build();
    try {
      SignedJWT signed = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
      JWSSigner signer = new MACSigner(secret);
      signed.sign(signer);
      return signed.serialize();
    } catch (JOSEException e) {
      throw new InvalidJwtException("failed to sign JWT", e);
    }
  }

  public JwtClaims verify(String token) {
    try {
      SignedJWT parsed = SignedJWT.parse(token);
      JWSVerifier verifier = new MACVerifier(secret);
      if (!parsed.verify(verifier)) {
        throw new InvalidJwtException("signature verification failed");
      }
      JWTClaimsSet claims = parsed.getJWTClaimsSet();
      Date exp = claims.getExpirationTime();
      if (exp != null && exp.toInstant().isBefore(Instant.now())) {
        throw new InvalidJwtException("token expired at " + exp.toInstant());
      }
      String audience = claims.getAudience().isEmpty() ? null : claims.getAudience().get(0);
      return new JwtClaims(
          claims.getSubject(),
          audience,
          claims.getIssuer(),
          claims.getIssueTime().toInstant(),
          exp == null ? null : exp.toInstant());
    } catch (ParseException | JOSEException e) {
      throw new InvalidJwtException("could not verify JWT: " + e.getMessage(), e);
    }
  }
}
