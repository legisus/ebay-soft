package com.ebaysoft.security.crypto;

/**
 * Thrown when {@link TokenCipher#decryptToString(byte[])} can't authenticate the ciphertext —
 * either the GCM tag fails to verify (tamper / wrong key) or the input shape is invalid.
 */
public final class InvalidCiphertextException extends RuntimeException {

  public InvalidCiphertextException(String reason, Throwable cause) {
    super(reason, cause);
  }

  public InvalidCiphertextException(String reason) {
    super(reason);
  }
}
