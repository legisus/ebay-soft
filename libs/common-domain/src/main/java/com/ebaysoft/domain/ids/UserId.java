package com.ebaysoft.domain.ids;

import java.util.Objects;
import java.util.UUID;

/** Identifier of an authenticated user — opaque UUID propagated via {@code X-User-Id}. */
public record UserId(UUID value) {

  public UserId {
    Objects.requireNonNull(value, "value");
  }

  public static UserId random() {
    return new UserId(UUID.randomUUID());
  }

  public static UserId fromString(String s) {
    return new UserId(UUID.fromString(s));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
