package com.ebaysoft.ebay.config;

import com.ebaysoft.security.crypto.TokenCipher;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Wires the {@link TokenCipher} from the KEK env var. Production: KEK is a sealed secret, 32
 * random bytes base64-encoded. Dev default boots the service with a static key (NOT for prod).
 */
@Configuration
public class CryptoConfig {

  @ConfigurationProperties(prefix = "ebay-soft.crypto")
  @Validated
  public record CryptoProperties(@NotBlank String tokenKekBase64) {}

  @Bean
  public TokenCipher tokenCipher(CryptoProperties props) {
    return TokenCipher.fromBase64(props.tokenKekBase64());
  }

  @Bean
  public CryptoProperties cryptoProperties(
      @org.springframework.beans.factory.annotation.Value("${ebay-soft.crypto.token-kek-base64}") String kek) {
    return new CryptoProperties(kek);
  }
}
