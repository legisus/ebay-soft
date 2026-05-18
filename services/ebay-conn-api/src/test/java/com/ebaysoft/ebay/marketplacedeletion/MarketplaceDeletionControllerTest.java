package com.ebaysoft.ebay.marketplacedeletion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.ebaysoft.events.cloudevent.CloudEvent;
import com.ebaysoft.events.publisher.EventPublisher;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class MarketplaceDeletionControllerTest {

  private static final String TOKEN = "test-verification-token-32-chars-long!!";
  private static final String ENDPOINT = "https://example.com/v1/ebay/marketplace-deletion";

  private final EventPublisher events = mock(EventPublisher.class);
  private final MarketplaceDeletionController controller =
      new MarketplaceDeletionController(new MarketplaceDeletionProperties(TOKEN, ENDPOINT), events);

  @Test
  void challenge_returns_sha256_of_challenge_token_endpoint() throws Exception {
    String challenge = "abc123";
    String expected = sha256Hex(challenge + TOKEN + ENDPOINT);

    StepVerifier.create(controller.challenge(challenge))
        .assertNext(body -> assertThat(body).containsEntry("challengeResponse", expected))
        .verifyComplete();
  }

  @Test
  void challenge_response_is_64_hex_chars() {
    StepVerifier.create(controller.challenge("any-input"))
        .assertNext(body -> {
          String resp = body.get("challengeResponse");
          assertThat(resp).hasSize(64).matches("[0-9a-f]{64}");
        })
        .verifyComplete();
  }

  @Test
  void notification_publishes_tenant_gdpr_purge_event() {
    StepVerifier.create(controller.notify(
            java.util.Map.of(
                "notification",
                java.util.Map.of("data", java.util.Map.of("username", "scammer42")))))
        .verifyComplete();

    org.mockito.ArgumentCaptor<CloudEvent> cap = org.mockito.ArgumentCaptor.forClass(CloudEvent.class);
    org.mockito.Mockito.verify(events).publish(cap.capture());
    CloudEvent event = cap.getValue();
    assertThat(event.type()).isEqualTo("ebay-soft.tenant.gdpr_purge.v1");
    assertThat(event.source()).isEqualTo("/ebay-conn-api");
    assertThat(event.subject()).isEqualTo("scammer42");
  }

  private static String sha256Hex(String input) throws Exception {
    return HexFormat.of()
        .formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
  }
}
