package com.ebaysoft.sync.ebay;

import com.ebaysoft.sync.config.EbayClientProperties;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Exponential-backoff retry for 429 / 5xx responses on outbound eBay calls. Caps retries per
 * configured limit so we never hammer eBay during sustained outages.
 *
 * <p>Idempotent GETs only — POST/PUT/DELETE callers must opt-in via their own retry policy with
 * idempotency keys.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EbayRetryFilter {

  private final EbayClientProperties props;

  public ExchangeFilterFunction asExchangeFilter() {
    return (req, next) -> {
      if (!"GET".equalsIgnoreCase(req.method().name())) {
        return next.exchange(req);
      }
      return next.exchange(req)
          .flatMap(resp -> {
            if (shouldRetry(resp)) {
              return Mono.error(new RetryableEbayResponseException(resp.statusCode().value()));
            }
            return Mono.just(resp);
          })
          .retryWhen(retrySpec())
          .onErrorResume(RetryableEbayResponseException.class, e ->
              Mono.error(new EbayUpstreamException(
                  "eBay still returning " + e.status() + " after " + props.retryMaxAttempts() + " retries",
                  e)));
    };
  }

  private boolean shouldRetry(ClientResponse resp) {
    HttpStatus status = HttpStatus.resolve(resp.statusCode().value());
    if (status == null) return false;
    return status == HttpStatus.TOO_MANY_REQUESTS || status.is5xxServerError();
  }

  private Retry retrySpec() {
    return Retry.backoff(props.retryMaxAttempts(), Duration.parse(props.retryBaseBackoff()))
        .filter(t -> t instanceof RetryableEbayResponseException)
        .doBeforeRetry(s ->
            log.atWarn()
                .addKeyValue("attempt", s.totalRetries() + 1)
                .log("retrying eBay request"));
  }

  private static final class RetryableEbayResponseException extends RuntimeException {
    private final int status;

    RetryableEbayResponseException(int status) {
      super("retryable eBay response: " + status);
      this.status = status;
    }

    int status() {
      return status;
    }
  }
}
