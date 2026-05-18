package com.ebaysoft.security.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class TokenCipherTest {

  private static final byte[] KEK = randomKey();
  private final TokenCipher cipher = new TokenCipher(KEK);

  @Test
  void round_trip_recovers_the_original_plaintext() {
    String secret = "ebay-refresh-token-v^FZ#mock-value-1234567890";
    byte[] encrypted = cipher.encrypt(secret);
    String decrypted = cipher.decryptToString(encrypted);
    assertThat(decrypted).isEqualTo(secret);
  }

  @Test
  void each_encryption_uses_a_fresh_nonce_so_ciphertexts_differ() {
    byte[] a = cipher.encrypt("same-secret");
    byte[] b = cipher.encrypt("same-secret");
    assertThat(a).isNotEqualTo(b);
  }

  @Test
  void tampered_ciphertext_is_rejected() {
    byte[] enc = cipher.encrypt("secret");
    enc[enc.length - 1] ^= 0x01; // flip last byte of GCM tag
    assertThatThrownBy(() -> cipher.decryptToString(enc))
        .isInstanceOf(InvalidCiphertextException.class);
  }

  @Test
  void wrong_key_is_rejected() {
    byte[] enc = cipher.encrypt("secret");
    TokenCipher otherKey = new TokenCipher(randomKey());
    assertThatThrownBy(() -> otherKey.decryptToString(enc))
        .isInstanceOf(InvalidCiphertextException.class);
  }

  @Test
  void constructor_rejects_keys_that_arent_256_bits() {
    assertThatThrownBy(() -> new TokenCipher(new byte[16]))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("32 bytes");
  }

  @Test
  void key_can_be_provided_as_base64_for_env_var_convenience() {
    String base64Key = Base64.getEncoder().encodeToString(KEK);
    TokenCipher fromBase64 = TokenCipher.fromBase64(base64Key);
    byte[] enc = cipher.encrypt("hello");
    assertThat(fromBase64.decryptToString(enc)).isEqualTo("hello");
  }

  private static byte[] randomKey() {
    byte[] k = new byte[32];
    new SecureRandom().nextBytes(k);
    return k;
  }
}
