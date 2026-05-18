package com.ebaysoft.web.errors;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Helpers to build RFC 7807 problem responses with consistent {@code type} URIs and trace context.
 * Replaces ad-hoc {@code Map<String, Object>} error payloads.
 */
public final class ProblemDetailFactory {

  private static final URI ABOUT_BLANK = URI.create("about:blank");

  private ProblemDetailFactory() {}

  public static ProblemDetail of(HttpStatus status, String title) {
    ProblemDetail pd = ProblemDetail.forStatus(status);
    pd.setTitle(title);
    pd.setType(ABOUT_BLANK);
    return pd;
  }

  public static ProblemDetail of(HttpStatus status, String title, String detail) {
    ProblemDetail pd = of(status, title);
    pd.setDetail(detail);
    return pd;
  }

  public static ProblemDetail badRequest(String detail) {
    return of(HttpStatus.BAD_REQUEST, "Bad Request", detail);
  }

  public static ProblemDetail conflict(String detail) {
    return of(HttpStatus.CONFLICT, "Conflict", detail);
  }

  public static ProblemDetail notFound(String detail) {
    return of(HttpStatus.NOT_FOUND, "Not Found", detail);
  }

  public static ProblemDetail unprocessable(String detail) {
    return of(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity", detail);
  }
}
