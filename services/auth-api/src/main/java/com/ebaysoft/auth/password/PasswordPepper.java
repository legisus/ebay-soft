package com.ebaysoft.auth.password;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * HMAC-SHA256 password pepper. Pre-processes the raw password before Argon2 hashing, so a
 * database-only compromise can't replay Argon2 verification without the pepper bytes (which live
 * outside the DB).
 *
 * <p>Versioned: every persisted hash records the pepper_version it was made under
 * ({@code users.pepper_version}). On login we read that version, recompute HMAC with the matching
 * pepper bytes, and pass the result to {@link
 * org.springframework.security.crypto.argon2.Argon2PasswordEncoder#matches}. Rotation is then a
 * matter of adding {@code v2} to the map and updating {@link #currentVersion}; old hashes stay
 * valid until those users next set a password.
 *
 * <p>Version 0 means "no pepper applied" — kept for forward-compat with rows created before this
 * class shipped. {@link #apply} returns the raw password unchanged for v0.
 */
@Component
public class PasswordPepper {

  private final Map<Integer, byte[]> pepperBytes;
  private final int currentVersion;

  public PasswordPepper(
      @Value("${ebay-soft.auth.password-pepper-v1}") String pepperV1Base64) {
    byte[] v1 = Base64.getDecoder().decode(pepperV1Base64);
    if (v1.length < 32) {
      throw new IllegalStateException(
          "ebay-soft.auth.password-pepper-v1 must decode to at least 32 bytes; got " + v1.length);
    }
    this.pepperBytes = Map.of(1, v1);
    this.currentVersion = 1;
  }

  /**
   * Apply the pepper for the given version and return the value to pass to Argon2. Version 0
   * skips pepper entirely (legacy rows).
   */
  public String apply(String rawPassword, int version) {
    if (version == 0) {
      return rawPassword;
    }
    byte[] pepper = pepperBytes.get(version);
    if (pepper == null) {
      throw new IllegalStateException("Unknown pepper version " + version);
    }
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(pepper, "HmacSHA256"));
      byte[] hmac = mac.doFinal(rawPassword.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hmac);
    } catch (java.security.GeneralSecurityException e) {
      throw new IllegalStateException("HMAC-SHA256 not available", e);
    }
  }

  public int currentVersion() {
    return currentVersion;
  }
}
