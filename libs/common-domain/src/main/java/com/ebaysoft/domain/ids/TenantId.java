package com.ebaysoft.domain.ids;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a tenant — opaque UUID propagated via {@code X-Tenant-Id}. */
public record TenantId(UUID value) {

  public TenantId {
    Objects.requireNonNull(value, "value");
  }

  public static TenantId random() {
    return new TenantId(UUID.randomUUID());
  }

  public static TenantId fromString(String s) {
    return new TenantId(UUID.fromString(s));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
