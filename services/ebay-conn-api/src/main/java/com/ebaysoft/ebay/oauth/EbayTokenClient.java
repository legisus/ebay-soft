package com.ebaysoft.ebay.oauth;

import com.ebaysoft.ebay.config.EbayProperties;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Calls eBay's token endpoint with the OAuth client credentials (HTTP Basic) and an authorization
 * code, returning the access + refresh tokens. eBay refresh tokens last 18 months by default,
 * access tokens 2 hours — see docs/EBAY_API.md.
 */
@Component
@Slf4j
public class EbayTokenClient {

  private final WebClient webClient;
  private final EbayProperties props;
  private final String basicAuth;

  public EbayTokenClient(EbayProperties props, WebClient.Builder builder) {
    this.props = props;
    this.webClient = builder.build();
    this.basicAuth =
        "Basic "
            + Base64.getEncoder()
                .encodeToString((props.clientId() + ":" + props.clientSecret()).getBytes(StandardCharsets.UTF_8));
  }

  public Mono<EbayTokenResponse> exchangeCode(String code) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "authorization_code");
    form.add("code", code);
    form.add("redirect_uri", props.redirectUri());

    return webClient
        .post()
        .uri(props.tokenUrl())
        .header(HttpHeaders.AUTHORIZATION, basicAuth)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(BodyInserters.fromFormData(form))
        .retrieve()
        .bodyToMono(EbayTokenResponse.class)
        .doOnNext(r -> log.atInfo()
            .addKeyValue("expiresIn", r.expiresIn())
            .log("eBay token exchange succeeded"));
  }
}
