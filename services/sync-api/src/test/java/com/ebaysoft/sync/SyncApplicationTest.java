package com.ebaysoft.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.ebaysoft.sync.status.SyncStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.r2dbc.R2dbcRepositoriesAutoConfiguration"
})
class SyncApplicationTest {

  @Autowired WebTestClient client;

  @Test
  void sync_status_snapshot_returns_idle_for_a_new_tenant() {
    UUID tenantId = UUID.randomUUID();
    SyncStatus status = client.get()
        .uri(uri -> uri.path("/v1/sync/status").queryParam("tenantId", tenantId).build())
        .exchange()
        .expectStatus().isOk()
        .expectBody(SyncStatus.class)
        .returnResult().getResponseBody();

    assertThat(status).isNotNull();
    assertThat(status.tenantId()).isEqualTo(tenantId);
    assertThat(status.phase()).isEqualTo("idle");
    assertThat(status.itemsProcessed()).isZero();
  }
}
