package com.ebaysoft.auth.tenant;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Write path for {@code auth.tenants}. Reads land when there's a multi-user feature. */
@Repository
@RequiredArgsConstructor
public class Tenants {

  private final JdbcTemplate jdbc;

  /** Create a tenant whose display name defaults to the owner email's local-part. */
  public UUID create(String name) {
    UUID id = UUID.randomUUID();
    jdbc.update("INSERT INTO tenants (id, name) VALUES (?, ?)", id, name);
    return id;
  }
}
