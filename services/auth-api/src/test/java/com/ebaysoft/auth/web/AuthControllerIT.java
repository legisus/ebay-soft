package com.ebaysoft.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ebaysoft.auth.support.AuthTestDatabase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end exercise of #162's auth surface: signup creates the tenant+user row pair, login
 * round-trips with the stored Argon2 hash, refresh rotates the long-lived token, and /me reflects
 * the caller back. All four wired against a real Postgres via {@link AuthTestDatabase}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIT extends AuthTestDatabase {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void cleanSchema() {
    jdbc.update("TRUNCATE refresh_tokens, users, tenants RESTART IDENTITY CASCADE");
  }

  @Test
  void signup_creates_tenant_and_user_and_returns_tokens() throws Exception {
    String body = """
        {"email": "owner@example.com", "password": "correct-horse-battery-staple"}
        """;

    MvcResult result =
        mvc.perform(post("/v1/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.access_token").isString())
            .andExpect(jsonPath("$.refresh_token").isString())
            .andExpect(jsonPath("$.tenant_id").isString())
            .andReturn();

    Integer tenantCount = jdbc.queryForObject("SELECT count(*) FROM tenants", Integer.class);
    Integer userCount = jdbc.queryForObject("SELECT count(*) FROM users", Integer.class);
    Integer refreshCount =
        jdbc.queryForObject("SELECT count(*) FROM refresh_tokens", Integer.class);
    assertThat(tenantCount).isEqualTo(1);
    assertThat(userCount).isEqualTo(1);
    assertThat(refreshCount).isEqualTo(1);

    String hash =
        jdbc.queryForObject(
            "SELECT password_hash FROM users WHERE email = 'owner@example.com'", String.class);
    assertThat(hash).startsWith("$argon2id$"); // Argon2id PHC string
    assertThat(hash).doesNotContain("correct-horse-battery-staple"); // not plaintext
  }

  @Test
  void login_validates_password_and_returns_fresh_tokens() throws Exception {
    signup("owner@example.com", "correct-horse-battery-staple");

    mvc.perform(
            post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email": "owner@example.com", "password": "correct-horse-battery-staple"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.access_token").isString())
        .andExpect(jsonPath("$.refresh_token").isString());
  }

  @Test
  void login_rejects_wrong_password_with_401() throws Exception {
    signup("owner@example.com", "correct-horse-battery-staple");

    mvc.perform(
            post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email": "owner@example.com", "password": "wrong"}
                    """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void refresh_rotates_the_token_and_invalidates_the_old_one() throws Exception {
    Tokens t = signup("owner@example.com", "correct-horse-battery-staple");

    MvcResult result =
        mvc.perform(
                post("/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"refresh_token\": \"" + t.refresh() + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").isString())
            .andExpect(jsonPath("$.refresh_token").isString())
            .andReturn();

    JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
    String newRefresh = body.get("refresh_token").asText();
    assertThat(newRefresh).isNotEqualTo(t.refresh()); // rotated

    // The original refresh token must no longer be usable.
    mvc.perform(
            post("/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refresh_token\": \"" + t.refresh() + "\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void me_returns_the_caller_when_bearer_token_is_valid() throws Exception {
    Tokens t = signup("owner@example.com", "correct-horse-battery-staple");

    mvc.perform(get("/v1/auth/me").header("Authorization", "Bearer " + t.access()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("owner@example.com"))
        .andExpect(jsonPath("$.tenant_id").value(t.tenantId()))
        .andExpect(jsonPath("$.role").value("owner"));
  }

  @Test
  void me_returns_401_without_bearer_token() throws Exception {
    mvc.perform(get("/v1/auth/me")).andExpect(status().isUnauthorized());
  }

  /** Helper: signs up a user and returns the issued tokens + tenant id. */
  private Tokens signup(String email, String password) throws Exception {
    String body = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    MvcResult result =
        mvc.perform(post("/v1/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andReturn();
    JsonNode json = mapper.readTree(result.getResponse().getContentAsString());
    return new Tokens(
        json.get("access_token").asText(),
        json.get("refresh_token").asText(),
        json.get("tenant_id").asText());
  }

  private record Tokens(String access, String refresh, String tenantId) {}
}
