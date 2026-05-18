package com.ebaysoft.events.cloudevent;

import java.time.Instant;
import java.util.UUID;

/**
 * Minimal CloudEvents 1.0 envelope — see https://cloudevents.io. Carries the data the rest of the
 * platform depends on, and intentionally omits OPTIONAL fields we don't use today
 * ({@code dataschema}, {@code datacontentencoding}).
 *
 * <p>{@code data} is a generic Object so the publisher can choose: a record, a Map, raw bytes. The
 * outbox layer serializes to JSON.
 */
public record CloudEvent(
    String id,
    String specversion,
    String type,
    String source,
    String subject,
    Instant time,
    String datacontenttype,
    Object data) {

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String id;
    private String type;
    private String source;
    private String subject;
    private Instant time;
    private String datacontenttype = "application/json";
    private Object data;

    public Builder id(String id) { this.id = id; return this; }
    public Builder type(String type) { this.type = type; return this; }
    public Builder source(String source) { this.source = source; return this; }
    public Builder subject(String subject) { this.subject = subject; return this; }
    public Builder time(Instant time) { this.time = time; return this; }
    public Builder datacontenttype(String contentType) { this.datacontenttype = contentType; return this; }
    public Builder data(Object data) { this.data = data; return this; }

    public CloudEvent build() {
      if (type == null || type.isBlank()) {
        throw new IllegalStateException("type is required");
      }
      if (source == null || source.isBlank()) {
        throw new IllegalStateException("source is required");
      }
      return new CloudEvent(
          id != null ? id : UUID.randomUUID().toString(),
          "1.0",
          type,
          source,
          subject,
          time != null ? time : Instant.now(),
          datacontenttype,
          data);
    }
  }
}
