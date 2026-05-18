package com.ebaysoft.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Springdoc config — exposes the served spec at {@code /v3/api-docs} and Swagger UI at
 * {@code /swagger-ui.html}. The committed {@code openapi.yaml} is the source of truth; CI
 * fails if the served document drifts from it ({@link com.ebaysoft.auth.OpenApiContractTest}).
 */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI authApiOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("auth-api")
                .version("0.1.0")
                .description("Tenants, users, sessions, MFA, JWT issuance."));
  }
}
