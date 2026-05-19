package com.ebaysoft.ebay.oauth;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Short-lived CSRF token minted by {@code /v1/oauth/ebay/start} and consumed by the callback. The
 * {@code state} value is the primary key — eBay echoes it back unchanged, so a single lookup
 * recovers the tenant + marketplace we were mid-flight for.
 *
 * <p>{@link Persistable} is implemented because R2DBC's heuristic for INSERT vs UPDATE is
 * "is @Id null?" — but our @Id is always set on creation, so without an explicit hint the repo
 * tries UPDATE on a row that doesn't yet exist. {@link #isNew()} returns true when
 * {@code createdAt} is null (the DB defaults it on insert), letting save() correctly INSERT.
 */
@Table("oauth_states")
public record OauthState(
    @Id String state,
    @Column("tenant_id") UUID tenantId,
    @Column("marketplace_id") String marketplaceId,
    @Column("redirect_uri") String redirectUri,
    @Column("created_at") Instant createdAt,
    @Column("expires_at") Instant expiresAt)
    implements Persistable<String> {

  public static OauthState minted(
      String state, UUID tenantId, String marketplaceId, String redirectUri, Instant expiresAt) {
    return new OauthState(state, tenantId, marketplaceId, redirectUri, null, expiresAt);
  }

  @Override
  public String getId() {
    return state;
  }

  @Override
  public boolean isNew() {
    return createdAt == null;
  }
}
