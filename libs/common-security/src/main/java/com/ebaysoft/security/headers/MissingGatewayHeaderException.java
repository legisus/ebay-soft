package com.ebaysoft.security.headers;

/** Raised when a service expected a gateway-propagated identity header but didn't get one. */
public final class MissingGatewayHeaderException extends RuntimeException {

  public MissingGatewayHeaderException(String headerName) {
    super("missing required gateway header: " + headerName);
  }
}
