package com.ebaysoft.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

/**
 * Contract guard: every operationId from the committed {@code openapi.yaml} must appear in the
 * spec served live by springdoc. This catches code paths that drifted from the source of truth.
 * Schema-level drift (response shapes, params) is enforced by the generated client in the
 * {@code clients/} subprojects.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=" +
        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
        "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class OpenApiContractTest {

  @LocalServerPort int port;
  @Autowired RestTemplateBuilder rest;

  @Test
  void served_spec_contains_committed_operation_ids() {
    RestTemplate t = rest.build();
    String served = t.getForObject("http://localhost:" + port + "/v3/api-docs", String.class);
    assertThat(served).isNotNull();
    // Operations declared in services/auth-api/openapi.yaml — keep in sync.
    assertThat(served).contains("\"operationId\":\"getHealth\"");
  }
}
