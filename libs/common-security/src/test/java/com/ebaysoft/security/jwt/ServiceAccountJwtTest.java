package com.ebaysoft.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ServiceAccountJwtTest {

  // 32+ bytes for HS256 per RFC 7518
  private static final String SECRET = "this-is-a-test-secret-at-least-256-bits-long";
  private final ServiceAccountJwt jwt = new ServiceAccountJwt(SECRET, "ebay-soft");

  @Test
  void issued_token_round_trips_subject_and_audience() {
    String token = jwt.issue("sync-api", "accounting-api", Duration.ofMinutes(5));
    JwtClaims claims = jwt.verify(token);
    assertThat(claims.subject()).isEqualTo("sync-api");
    assertThat(claims.audience()).isEqualTo("accounting-api");
    assertThat(claims.issuer()).isEqualTo("ebay-soft");
  }

  @Test
  void tampered_token_is_rejected() {
    String token = jwt.issue("sync-api", "accounting-api", Duration.ofMinutes(5));
    String tampered = token.substring(0, token.length() - 4) + "AAAA";
    assertThatThrownBy(() -> jwt.verify(tampered)).isInstanceOf(InvalidJwtException.class);
  }

  @Test
  void expired_token_is_rejected() {
    // Issue with negative TTL — instantly expired
    String token = jwt.issue("sync-api", "accounting-api", Duration.ofSeconds(-1));
    assertThatThrownBy(() -> jwt.verify(token))
        .isInstanceOf(InvalidJwtException.class)
        .hasMessageContaining("expired");
  }

  @Test
  void wrong_secret_rejects_verification() {
    String token = jwt.issue("a", "b", Duration.ofMinutes(5));
    ServiceAccountJwt wrong = new ServiceAccountJwt("another-test-secret-also-256-bits-or-longer-here", "ebay-soft");
    assertThatThrownBy(() -> wrong.verify(token)).isInstanceOf(InvalidJwtException.class);
  }

  @Test
  void issuedAt_and_expiresAt_are_close_to_now() {
    Instant before = Instant.now();
    String token = jwt.issue("a", "b", Duration.ofMinutes(5));
    JwtClaims claims = jwt.verify(token);
    assertThat(claims.issuedAt()).isBetween(before.minusSeconds(1), Instant.now().plusSeconds(1));
    assertThat(claims.expiresAt()).isAfter(claims.issuedAt());
  }
}
