package com.ebaysoft.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless security for the Internal Demo milestone (#162). /signup, /login and /refresh are
 * public; /me does its own bearer-token check inside the controller (since per-service JWT
 * validation at api-gateway is a follow-up — see #162 acceptance criteria).
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
