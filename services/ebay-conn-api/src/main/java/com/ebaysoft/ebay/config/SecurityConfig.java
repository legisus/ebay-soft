package com.ebaysoft.ebay.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Bootstrap reactive security — Phase 1 leaves OAuth endpoints public (they ARE the auth flow);
 * future commits move tenant-scoped endpoints behind the gateway-propagated JWT.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  @Bean
  public SecurityWebFilterChain filter(ServerHttpSecurity http) {
    return http.csrf(csrf -> csrf.disable())
        .authorizeExchange(req -> req.anyExchange().permitAll())
        .build();
  }
}
