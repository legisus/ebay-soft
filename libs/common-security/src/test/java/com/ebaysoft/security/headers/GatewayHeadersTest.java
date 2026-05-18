package com.ebaysoft.security.headers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ebaysoft.domain.ids.TenantId;
import com.ebaysoft.domain.ids.UserId;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GatewayHeadersTest {

  @Test
  void parses_tenant_and_user_ids_from_gateway_headers() {
    UUID tenant = UUID.randomUUID();
    UUID user = UUID.randomUUID();
    Map<String, String> headers =
        Map.of(
            "X-Tenant-Id", tenant.toString(),
            "X-User-Id", user.toString());

    GatewayPrincipal p = GatewayHeaders.parse(headers::get);

    assertThat(p.tenantId()).isEqualTo(new TenantId(tenant));
    assertThat(p.userId()).isEqualTo(new UserId(user));
  }

  @Test
  void header_lookup_is_case_insensitive() {
    UUID tenant = UUID.randomUUID();
    Map<String, String> headers = Map.of("x-tenant-id", tenant.toString());
    GatewayPrincipal p = GatewayHeaders.parse(headers::get);
    assertThat(p.tenantId()).isEqualTo(new TenantId(tenant));
    assertThat(p.userId()).isNull();
  }

  @Test
  void missing_tenant_id_returns_null_tenant() {
    GatewayPrincipal p = GatewayHeaders.parse(name -> null);
    assertThat(p.tenantId()).isNull();
    assertThat(p.userId()).isNull();
  }

  @Test
  void requireTenant_throws_when_tenant_missing() {
    GatewayPrincipal p = GatewayHeaders.parse(name -> null);
    assertThatThrownBy(p::requireTenant)
        .isInstanceOf(MissingGatewayHeaderException.class)
        .hasMessageContaining("X-Tenant-Id");
  }

  @Test
  void malformed_uuid_throws() {
    Map<String, String> headers = Map.of("X-Tenant-Id", "not-a-uuid");
    assertThatThrownBy(() -> GatewayHeaders.parse(headers::get))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
