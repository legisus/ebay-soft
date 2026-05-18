package com.ebaysoft.sync.ebay;

import com.ebaysoft.sync.config.EbayClientProperties;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

/**
 * Builds the WebClient used by sync-api for every eBay call. Wires the connection pool, timeouts,
 * retry filter, and per-tenant rate-limit filter from {@link EbayClientProperties}.
 */
@Configuration
@EnableConfigurationProperties(EbayClientProperties.class)
public class EbayWebClientConfig {

  @Bean
  public WebClient ebayWebClient(
      EbayClientProperties props,
      EbayRateLimitFilter rateLimitFilter,
      EbayRetryFilter retryFilter) {

    ConnectionProvider pool =
        ConnectionProvider.builder("ebay")
            .maxConnections(props.maxConnections())
            .pendingAcquireMaxCount(props.maxConnections() * 10)
            .pendingAcquireTimeout(props.connectTimeoutDuration())
            .build();

    HttpClient http =
        HttpClient.create(pool)
            .responseTimeout(props.readTimeoutDuration())
            .option(
                ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) props.connectTimeoutDuration().toMillis())
            .doOnConnected(c -> c.addHandlerLast(
                new ReadTimeoutHandler(props.readTimeoutDuration().toMillis(), TimeUnit.MILLISECONDS)))
            .compress(true);

    return WebClient.builder()
        .baseUrl(props.baseUrl())
        .clientConnector(new ReactorClientHttpConnector(http))
        .defaultHeader(HttpHeaders.ACCEPT, "application/json")
        .filter(rateLimitFilter.asExchangeFilter())
        .filter(retryFilter.asExchangeFilter())
        .build();
  }
}
