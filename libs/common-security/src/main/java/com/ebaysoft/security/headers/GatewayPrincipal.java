package com.ebaysoft.security.headers;

import com.ebaysoft.domain.ids.TenantId;
import com.ebaysoft.domain.ids.UserId;

/**
 * Identity propagated from the API gateway to downstream services via {@code X-Tenant-Id} and
 * {@code X-User-Id} headers. Either or both may be {@code null} for anonymous calls (sign-up, login,
 * webhooks). Services that require a tenant should call {@link #requireTenant()}.
 */
public record GatewayPrincipal(TenantId tenantId, UserId userId) {

  public TenantId requireTenant() {
    if (tenantId == null) {
      throw new MissingGatewayHeaderException("X-Tenant-Id");
    }
    return tenantId;
  }

  public UserId requireUser() {
    if (userId == null) {
      throw new MissingGatewayHeaderException("X-User-Id");
    }
    return userId;
  }
}
