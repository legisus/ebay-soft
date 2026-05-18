package com.ebaysoft.ebay;

import static org.assertj.core.api.Assertions.assertThat;

import com.ebaysoft.ebay.accounts.EbayAccountRepository;
import com.ebaysoft.ebay.oauth.EbayConnectionService;
import com.ebaysoft.ebay.oauth.EbayOauthController.StartResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Phase 1 smoke tests — boot the reactive context, hit the two public endpoints that don't need
 * a real Postgres. The full callback flow lives in {@code EbayOauthCallbackIT} once Testcontainers
 * lands; here we mock the orchestrator + repository so the context can wire.
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

  @Test
  void marketplace_deletion_handshake_echoes_the_challenge_code() {
    client.get().uri("/v1/ebay/marketplace-deletion?challenge_code=abc123")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.challengeResponse").isEqualTo("abc123");
  }

  @Test
  void oauth_start_returns_authorize_url_with_state() {
    StartResponse body = client.get().uri("/v1/oauth/ebay/start")
        .exchange()
        .expectStatus().isOk()
        .expectBody(StartResponse.class)
        .returnResult().getResponseBody();

    assertThat(body).isNotNull();
    assertThat(body.authorizeUrl()).startsWith("https://auth.sandbox.ebay.com/oauth2/authorize");
    assertThat(body.authorizeUrl()).contains("response_type=code");
    assertThat(body.authorizeUrl()).contains("client_id=");
    assertThat(body.authorizeUrl()).contains("state=" + body.state());
    assertThat(body.state()).isNotBlank();
  }
}
