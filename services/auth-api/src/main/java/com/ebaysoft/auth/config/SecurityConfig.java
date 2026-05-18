package com.ebaysoft.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Bootstrap security config. The full sign-up/login/JWT issuance flow lands when issue #26's
 * production endpoints are implemented; for now the auth-api only exposes {@code /v1/health} and
 * the Actuator endpoints, both unauthenticated for Phase 0 smoke tests.
 */
@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filter(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(req -> req.anyRequest().permitAll())
        .build();
  }
}
