package com.ebaysoft.security.headers;

import com.ebaysoft.domain.ids.TenantId;
import com.ebaysoft.domain.ids.UserId;
import java.util.UUID;
import java.util.function.Function;

/**
 * Parses the gateway-propagated identity headers from any HTTP framework: the caller supplies a
 * lookup function (e.g. {@code request::getHeader}). Header names are matched case-insensitively
 * because HTTP/2 lowercases them and HTTP/1 servers vary.
 */
public final class GatewayHeaders {

  public static final String TENANT_HEADER = "X-Tenant-Id";
  public static final String USER_HEADER = "X-User-Id";

  private GatewayHeaders() {}

  public static GatewayPrincipal parse(Function<String, String> headerLookup) {
    TenantId tenantId = parseUuid(headerLookup, TENANT_HEADER, TenantId::new);
    UserId userId = parseUuid(headerLookup, USER_HEADER, UserId::new);
    return new GatewayPrincipal(tenantId, userId);
  }

  private static <T> T parseUuid(
      Function<String, String> lookup, String name, Function<UUID, T> wrap) {
    String raw = lookup.apply(name);
    if (raw == null) {
      raw = lookup.apply(name.toLowerCase());
    }
    if (raw == null) {
      raw = lookup.apply(name.toUpperCase());
    }
    return raw == null ? null : wrap.apply(UUID.fromString(raw));
  }
}
