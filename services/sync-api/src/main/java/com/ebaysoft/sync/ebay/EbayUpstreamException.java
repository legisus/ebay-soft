package com.ebaysoft.sync.ebay;

/** Thrown by sync-api when an outbound eBay call exhausts retries or fails irrecoverably. */
public final class EbayUpstreamException extends RuntimeException {
  public EbayUpstreamException(String message, Throwable cause) {
    super(message, cause);
  }
}
