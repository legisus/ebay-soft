package com.ebaysoft.auth.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Bootstraps a Postgres database for the auth-api Spring tests.
 *
 * <p>Two paths:
 *
 * <ul>
 *   <li><b>Default (CI):</b> spin up a Testcontainers postgres:16-alpine and point Spring's
 *       datasource at it.
 *   <li><b>Override (laptops without Docker):</b> set {@code TEST_PG_URL} to a reachable JDBC URL;
 *       the test will skip the container and use that DB instead. Same pattern we use for
 *       {@code libs/common-events} integration tests.
 * </ul>
 */
public abstract class AuthTestDatabase {

  private static final PostgreSQLContainer<?> CONTAINER;

  static {
    String overrideUrl = System.getenv("TEST_PG_URL");
    if (overrideUrl == null || overrideUrl.isBlank()) {
      CONTAINER = new PostgreSQLContainer<>("postgres:16-alpine");
      CONTAINER.start();
    } else {
      CONTAINER = null;
    }
  }

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    if (CONTAINER != null) {
      registry.add("spring.datasource.url", CONTAINER::getJdbcUrl);
      registry.add("spring.datasource.username", CONTAINER::getUsername);
      registry.add("spring.datasource.password", CONTAINER::getPassword);
    } else {
      registry.add("spring.datasource.url", () -> System.getenv("TEST_PG_URL"));
      registry.add(
          "spring.datasource.username",
          () ->
              System.getenv().getOrDefault("TEST_PG_USER", System.getProperty("user.name")));
      registry.add(
          "spring.datasource.password",
          () -> System.getenv().getOrDefault("TEST_PG_PASS", ""));
    }
    // Each test class boots into a clean auth schema; Flyway runs V001 + V002 on startup.
    registry.add("spring.flyway.clean-disabled", () -> "false");
  }
}
