package com.ebaysoft.auth.token;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Server-side refresh-token store. Only SHA-256 digests are persisted; the raw value is shown to
 * the client once at issue/rotate time and never re-derivable from the database alone.
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokens {

  private final JdbcTemplate jdbc;

  /** Persist a freshly-issued refresh token for a user. */
  public void store(UUID userId, String rawToken, Instant expiresAt) {
    jdbc.update(
        "INSERT INTO refresh_tokens (user_id, token_hash, expires_at) VALUES (?, ?, ?)",
        userId,
        sha256(rawToken),
        java.sql.Timestamp.from(expiresAt));
  }

  /** Find the (user, expiry) for a token that is still active. */
  public Optional<Active> findActive(String rawToken) {
    try {
      return Optional.ofNullable(
          jdbc.queryForObject(
              """
              SELECT user_id, expires_at FROM refresh_tokens
               WHERE token_hash = ? AND revoked_at IS NULL AND expires_at > now()
              """,
              (rs, n) ->
                  new Active(
                      (UUID) rs.getObject("user_id"),
                      rs.getTimestamp("expires_at").toInstant()),
              sha256(rawToken)));
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  /** Mark a refresh token revoked. Called on rotate (refresh endpoint) and explicit logout. */
  public void revoke(String rawToken) {
    jdbc.update(
        "UPDATE refresh_tokens SET revoked_at = now() WHERE token_hash = ? AND revoked_at IS NULL",
        sha256(rawToken));
  }

  public record Active(UUID userId, Instant expiresAt) {}

  private static byte[] sha256(String input) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(input.getBytes());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("JVM is missing SHA-256", e);
    }
  }
}
