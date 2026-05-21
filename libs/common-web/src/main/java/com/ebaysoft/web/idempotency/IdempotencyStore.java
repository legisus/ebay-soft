package com.ebaysoft.web.idempotency;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * JDBC-backed store for the {@code idempotency_keys} table (DDL: {@link IdempotencyDdl}). One
 * instance per service; takes a {@link DataSource} so it works outside Spring contexts too.
 *
 * <p>Three operations cover the request lifecycle:
 *
 * <ol>
 *   <li>{@link #findRecorded} on inbound request — if present, return the cached response and skip
 *       the handler.
 *   <li>{@link #record} after the handler responds with 2xx — store the outcome.
 *   <li>The filter that wires this up enforces 409 when the request_hash mismatches a stored key.
 * </ol>
 *
 * <p>This class is intentionally framework-free — the WebFilter that adapts an
 * HttpServletRequest into these calls lives per-service (or in a follow-up filter helper),
 * because Spring MVC vs WebFlux vs other entrypoints have very different request shapes.
 */
public class IdempotencyStore {

  private static final Duration DEFAULT_TTL = Duration.ofHours(24);

  private final JdbcTemplate jdbc;
  private final Duration ttl;

  public IdempotencyStore(DataSource dataSource) {
    this(dataSource, DEFAULT_TTL);
  }

  public IdempotencyStore(DataSource dataSource, Duration ttl) {
    this.jdbc = new JdbcTemplate(dataSource);
    this.ttl = ttl;
  }

  /** Returns the recorded response if a key exists and hasn't expired; empty otherwise. */
  public Optional<Recorded> findRecorded(UUID tenantId, String key) {
    try {
      return Optional.ofNullable(
          jdbc.queryForObject(
              """
              SELECT request_hash, response_status, response_body
              FROM idempotency_keys
              WHERE tenant_id = ? AND key = ? AND expires_at > now()
              """,
              (rs, n) ->
                  new Recorded(
                      rs.getBytes("request_hash"),
                      rs.getInt("response_status"),
                      rs.getString("response_body")),
              tenantId,
              key));
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  /** Persist the outcome of a successful (2xx) request. Caller hashes the request body. */
  public void record(
      UUID tenantId, String key, byte[] requestHash, int responseStatus, String responseBodyJson) {
    jdbc.update(
        """
        INSERT INTO idempotency_keys
            (tenant_id, key, request_hash, response_status, response_body, expires_at)
        VALUES (?, ?, ?, ?, ?::jsonb, ?)
        ON CONFLICT (tenant_id, key) DO NOTHING
        """,
        tenantId,
        key,
        requestHash,
        responseStatus,
        responseBodyJson,
        java.sql.Timestamp.from(Instant.now().plus(ttl)));
  }

  /** Best-effort cleanup, called by a scheduled task; returns the number of rows reaped. */
  public int reapExpired() {
    return jdbc.update("DELETE FROM idempotency_keys WHERE expires_at <= now()");
  }

  /** Canonical request hash: SHA-256 over method + "\\n" + path + "\\n" + body bytes. */
  public static byte[] requestHash(String method, String path, byte[] body) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(method.getBytes());
      md.update((byte) '\n');
      md.update(path.getBytes());
      md.update((byte) '\n');
      if (body != null) md.update(body);
      return md.digest();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("JVM is missing SHA-256", e);
    }
  }

  /** Whether two hashes are equal in constant time (no early-return on mismatch). */
  public static boolean hashesMatch(byte[] a, byte[] b) {
    return MessageDigest.isEqual(a, b);
  }

  public record Recorded(byte[] requestHash, int responseStatus, String responseBody) {}
}
