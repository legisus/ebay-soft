package com.ebaysoft.security.jwt;

/** Thrown when a presented JWT can't be parsed, has the wrong signature, or has expired. */
public final class InvalidJwtException extends RuntimeException {

  public InvalidJwtException(String reason) {
    super(reason);
  }

  public InvalidJwtException(String reason, Throwable cause) {
    super(reason, cause);
  }
}
