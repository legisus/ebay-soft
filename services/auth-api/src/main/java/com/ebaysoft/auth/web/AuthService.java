package com.ebaysoft.auth.web;

import com.ebaysoft.auth.password.PasswordPepper;
import com.ebaysoft.auth.tenant.Tenants;
import com.ebaysoft.auth.token.AuthTokens;
import com.ebaysoft.auth.token.RefreshTokens;
import com.ebaysoft.auth.user.Users;
import com.ebaysoft.auth.user.Users.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the signup / login / refresh flows. Argon2 hashing + token issuance live here;
 * controllers stay thin.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

  private final Tenants tenants;
  private final Users users;
  private final RefreshTokens refreshTokens;
  private final AuthTokens authTokens;
  private final PasswordPepper pepper;
  private final Argon2PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

  /** Atomic: create tenant + first user + first refresh token in one transaction. */
  @Transactional
  public TokenBundle signup(String email, String rawPassword) {
    String tenantName = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
    UUID tenantId = tenants.create(tenantName);
    int version = pepper.currentVersion();
    String hash = encoder.encode(pepper.apply(rawPassword, version));
    UUID userId = users.create(tenantId, email, hash, version);
    return issueTokens(userId, tenantId, email);
  }

  /**
   * Validate password and issue a fresh pair on success. Reads the user's stored {@code
   * pepper_version} and applies the matching HMAC pepper before Argon2 verification — so rows
   * created under any past pepper version still authenticate.
   */
  @Transactional
  public Optional<TokenBundle> login(String email, String rawPassword) {
    Optional<User> maybe = users.findByEmail(email);
    if (maybe.isEmpty()) {
      return Optional.empty();
    }
    User u = maybe.get();
    if (u.passwordHash() == null
        || !encoder.matches(pepper.apply(rawPassword, u.pepperVersion()), u.passwordHash())) {
      return Optional.empty();
    }
    users.touchLastLogin(u.id());
    return Optional.of(issueTokens(u.id(), u.tenantId(), u.email()));
  }

  /** Rotate a refresh token. Returns empty when the supplied token is invalid/expired/revoked. */
  @Transactional
  public Optional<TokenBundle> refresh(String rawRefreshToken) {
    Optional<RefreshTokens.Active> active = refreshTokens.findActive(rawRefreshToken);
    if (active.isEmpty()) {
      return Optional.empty();
    }
    Optional<User> maybeUser = users.findById(active.get().userId());
    if (maybeUser.isEmpty()) {
      return Optional.empty();
    }
    refreshTokens.revoke(rawRefreshToken);
    User u = maybeUser.get();
    return Optional.of(issueTokens(u.id(), u.tenantId(), u.email()));
  }

  private TokenBundle issueTokens(UUID userId, UUID tenantId, String email) {
    String access = authTokens.issueAccess(userId, tenantId, email);
    String refresh = authTokens.newRefresh();
    refreshTokens.store(userId, refresh, Instant.now().plus(AuthTokens.REFRESH_TTL));
    return new TokenBundle(access, refresh, tenantId);
  }

  public record TokenBundle(String access, String refresh, UUID tenantId) {}
}
