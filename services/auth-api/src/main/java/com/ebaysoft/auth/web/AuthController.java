package com.ebaysoft.auth.web;

import com.ebaysoft.auth.token.AuthTokens;
import com.ebaysoft.auth.user.Users;
import com.ebaysoft.auth.user.Users.User;
import com.ebaysoft.auth.web.AuthService.TokenBundle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public auth surface for the Internal Demo milestone (#162). No email verification, no MFA,
 * no idempotency-key handling yet — those are tracked separately (#159, #161).
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService auth;
  private final AuthTokens tokens;
  private final Users users;

  @PostMapping("/signup")
  public ResponseEntity<TokenResponse> signup(@Valid @RequestBody SignupRequest req) {
    TokenBundle bundle = auth.signup(req.email(), req.password());
    return ResponseEntity.status(HttpStatus.CREATED).body(TokenResponse.of(bundle));
  }

  @PostMapping("/login")
  public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
    return auth.login(req.email(), req.password())
        .map(bundle -> ResponseEntity.ok(TokenResponse.of(bundle)))
        .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
  }

  @PostMapping("/refresh")
  public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest req) {
    return auth.refresh(req.refresh_token())
        .map(bundle -> ResponseEntity.ok(TokenResponse.of(bundle)))
        .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
  }

  @GetMapping("/me")
  public ResponseEntity<MeResponse> me(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    try {
      AuthTokens.AccessClaims claims = tokens.verifyAccess(authorization.substring(7));
      Optional<User> u = users.findById(claims.userId());
      return u.map(user -> ResponseEntity.ok(new MeResponse(
              user.id().toString(), user.tenantId().toString(), user.email(), user.role())))
          .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    } catch (AuthTokens.InvalidAccessTokenException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
  }

  /* ----- DTOs ----- */

  public record SignupRequest(
      @Email @NotBlank String email,
      @NotBlank @Size(min = 12, max = 256) String password) {}

  public record LoginRequest(
      @Email @NotBlank String email,
      @NotBlank String password) {}

  public record RefreshRequest(@NotBlank String refresh_token) {}

  public record TokenResponse(String access_token, String refresh_token, String tenant_id) {
    static TokenResponse of(TokenBundle b) {
      return new TokenResponse(b.access(), b.refresh(), b.tenantId().toString());
    }
  }

  public record MeResponse(String id, String tenant_id, String email, String role) {}
}
