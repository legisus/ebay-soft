package com.ebaysoft.sync.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Bootstrap reactive security. Phase 1 leaves the sync surface open behind the gateway;
 * gateway-propagated JWT validation lands in {@link com.ebaysoft.security.headers.GatewayHeaders}-
 * backed auth when sync-api starts taking real tenant traffic.
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
