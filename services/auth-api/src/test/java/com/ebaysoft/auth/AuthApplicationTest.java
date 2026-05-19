package com.ebaysoft.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.ebaysoft.auth.support.AuthTestDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthApplicationTest extends AuthTestDatabase {

  @LocalServerPort int port;
  @Autowired RestTemplateBuilder rest;

  @Test
  void health_endpoint_returns_ok() {
    RestTemplate t = rest.build();
    String body = t.getForObject("http://localhost:" + port + "/v1/health", String.class);
    assertThat(body).contains("\"status\":\"ok\"").contains("\"service\":\"auth-api\"");
  }
}
