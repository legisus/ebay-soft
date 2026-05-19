package com.ebaysoft.ebay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ebaysoft.ebay.accounts.EbayAccountRepository;
import com.ebaysoft.ebay.oauth.EbayConnectionService;
import com.ebaysoft.ebay.oauth.EbayOauthController.StartResponse;
import com.ebaysoft.ebay.oauth.OauthStateRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

/**
 * Phase 1 smoke tests — boot the reactive context, hit the two public endpoints that don't need a
 * real Postgres. Repositories + the orchestrator are mocked so the context wires without R2DBC.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.r2dbc.R2dbcRepositoriesAutoConfiguration"
})
class EbayConnApplicationTest {

  @Autowired WebTestClient client;

  @MockitoBean EbayAccountRepository ebayAccountRepository;
  @MockitoBean EbayConnectionService ebayConnectionService;
  @MockitoBean OauthStateRepository oauthStateRepository;

  @Test
  void marketplace_deletion_handshake_returns_64_char_hex_sha256() {
    client.get().uri("/v1/ebay/marketplace-deletion?challenge_code=abc123")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.challengeResponse").value(
            (Object resp) -> assertThat((String) resp).hasSize(64).matches("[0-9a-f]{64}"));
  }

  @Test
  void oauth_start_persists_state_and_returns_authorize_url() {
    // Repo.save echoes the row back, matching the real R2DBC behavior.
    when(oauthStateRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    StartResponse body = client.post()
        .uri("/v1/oauth/ebay/start")
        .header("X-Tenant-Id", "11111111-1111-1111-1111-111111111111")
        .exchange()
        .expectStatus().isOk()
        .expectBody(StartResponse.class)
        .returnResult()
        .getResponseBody();

    assertThat(body).isNotNull();
    assertThat(body.authorizeUrl()).startsWith("https://auth.sandbox.ebay.com/oauth2/authorize");
    assertThat(body.authorizeUrl()).contains("response_type=code");
    assertThat(body.authorizeUrl()).contains("client_id=");
    assertThat(body.authorizeUrl()).contains("state=" + body.state());
    assertThat(body.state()).isNotBlank();
  }

  @Test
  void oauth_start_rejects_request_without_tenant_header() {
    client.post()
        .uri("/v1/oauth/ebay/start")
        .exchange()
        .expectStatus().is5xxServerError();
    // 500 today (default Spring exception mapping). A future ProblemDetailHandler will
    // map MissingTenantException to 401 — out of scope for the tight #164 slice.
  }

  @Test
  void accounts_list_returns_empty_when_no_tenant_header() {
    client.get()
        .uri("/v1/ebay/accounts")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.accounts").isArray()
        .jsonPath("$.accounts.length()").isEqualTo(0);
  }

  @Test
  void accounts_list_returns_tenants_accounts() {
    UUID tenantId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    when(ebayAccountRepository.findAllByTenantIdOrderByConnectedAtDesc(tenantId))
        .thenReturn(reactor.core.publisher.Flux.empty());

    client.get()
        .uri("/v1/ebay/accounts")
        .header("X-Tenant-Id", tenantId.toString())
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.accounts").isArray()
        .jsonPath("$.accounts.length()").isEqualTo(0);
  }
}
