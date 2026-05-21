package com.ebaysoft.auth.user;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Read + write paths for {@code auth.users}. Plain JDBC — no JPA in this service. */
@Repository
@RequiredArgsConstructor
public class Users {

  private final JdbcTemplate jdbc;

  /** Create the first user for a brand-new tenant. The tenant row is inserted by the caller. */
  public UUID create(UUID tenantId, String email, String passwordHash, int pepperVersion) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO users (id, tenant_id, email, password_hash, pepper_version) VALUES (?, ?, ?, ?, ?)",
        id,
        tenantId,
        email,
        passwordHash,
        pepperVersion);
    return id;
  }

  public Optional<User> findByEmail(String email) {
    return queryOne(
        "SELECT id, tenant_id, email, password_hash, role, pepper_version FROM users WHERE email = ?",
        email);
  }

  public Optional<User> findById(UUID id) {
    return queryOne(
        "SELECT id, tenant_id, email, password_hash, role, pepper_version FROM users WHERE id = ?",
        id);
  }

  public void touchLastLogin(UUID userId) {
    jdbc.update("UPDATE users SET last_login_at = now() WHERE id = ?", userId);
  }

  private Optional<User> queryOne(String sql, Object key) {
    try {
      return Optional.ofNullable(
          jdbc.queryForObject(
              sql,
              (rs, n) ->
                  new User(
                      (UUID) rs.getObject("id"),
                      (UUID) rs.getObject("tenant_id"),
                      rs.getString("email"),
                      rs.getString("password_hash"),
                      rs.getString("role"),
                      rs.getInt("pepper_version")),
              key));
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  public record User(
      UUID id, UUID tenantId, String email, String passwordHash, String role, int pepperVersion) {}
}
