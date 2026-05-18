package com.ebaysoft.web.errors;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Stub auto-configuration that ensures the {@link ProblemDetailFactory} is on the classpath in any
 * service that depends on common-web. No beans needed for the static helper itself; this hook lets
 * us add cross-cutting advice (e.g. a default {@code @RestControllerAdvice}) without bumping
 * consumers.
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.web.bind.annotation.RestControllerAdvice")
public class ProblemDetailAutoConfiguration {

  @Bean
  public ProblemDetailMarker problemDetailMarker() {
    return new ProblemDetailMarker();
  }

  /** Marker bean — its presence signals to integration tests that common-web auto-config ran. */
  public static final class ProblemDetailMarker {}
}
