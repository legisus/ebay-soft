package com.ebaysoft.security.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-256-GCM helper for symmetric encryption of small secrets (OAuth refresh tokens, TOTP seeds,
 * etc.). The 32-byte key (KEK) is supplied by the deploy layer via an env var; this class never
 * persists the key.
 *
 * <p>Wire format of {@link #encrypt(String)} output: {@code [12-byte nonce][ciphertext+16-byte
 * GCM tag]}. Self-contained — the recipient just needs the same KEK to decrypt.
 *
 * <p>See docs/SECURITY.md → Token storage.
 */
public final class TokenCipher {

  private static final int NONCE_LEN = 12; // GCM standard, never reuse with same key
  private static final int TAG_LEN_BITS = 128;
  private static final String TRANSFORM = "AES/GCM/NoPadding";

  private final SecretKeySpec key;
  private final SecureRandom rng = new SecureRandom();

  public TokenCipher(byte[] key) {
    if (key == null || key.length != 32) {
      throw new IllegalArgumentException("KEK must be 32 bytes (AES-256); got " + (key == null ? "null" : key.length));
    }
    this.key = new SecretKeySpec(key, "AES");
  }

  public static TokenCipher fromBase64(String base64) {
    return new TokenCipher(Base64.getDecoder().decode(base64));
  }

  public byte[] encrypt(String plaintext) {
    byte[] nonce = new byte[NONCE_LEN];
    rng.nextBytes(nonce);
    try {
      Cipher cipher = Cipher.getInstance(TRANSFORM);
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LEN_BITS, nonce));
      byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      return ByteBuffer.allocate(nonce.length + ct.length).put(nonce).put(ct).array();
    } catch (Exception e) {
      throw new IllegalStateException("AES-GCM encryption failed", e);
    }
  }

  public String decryptToString(byte[] ciphertext) {
    if (ciphertext == null || ciphertext.length < NONCE_LEN + 16) {
      throw new InvalidCiphertextException("ciphertext too short");
    }
    byte[] nonce = new byte[NONCE_LEN];
    byte[] ct = new byte[ciphertext.length - NONCE_LEN];
    System.arraycopy(ciphertext, 0, nonce, 0, NONCE_LEN);
    System.arraycopy(ciphertext, NONCE_LEN, ct, 0, ct.length);
    try {
      Cipher cipher = Cipher.getInstance(TRANSFORM);
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LEN_BITS, nonce));
      byte[] pt = cipher.doFinal(ct);
      return new String(pt, StandardCharsets.UTF_8);
    } catch (javax.crypto.AEADBadTagException e) {
      throw new InvalidCiphertextException("GCM tag verification failed", e);
    } catch (Exception e) {
      throw new InvalidCiphertextException("AES-GCM decryption failed: " + e.getMessage(), e);
    }
  }
}
