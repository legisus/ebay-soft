package com.ebaysoft.ebay.accounts;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** R2DBC entity for {@code ebay_conn.ebay_accounts} — one row per connected eBay seller account. */
@Table("ebay_accounts")
public record EbayAccount(
    @Id UUID id,
    @Column("tenant_id") UUID tenantId,
    @Column("marketplace_id") String marketplaceId,
    @Column("ebay_user_id") String ebayUserId,
    @Column("refresh_token_enc") byte[] refreshTokenEnc,
    @Column("access_token_expires_at") Instant accessTokenExpiresAt,
    @Column("status") String status,
    @Column("connected_at") Instant connectedAt) {

  public static EbayAccount newlyConnected(
      UUID tenantId, String marketplaceId, String ebayUserId, byte[] refreshTokenEnc, Instant accessExpiresAt) {
    return new EbayAccount(
        null, tenantId, marketplaceId, ebayUserId, refreshTokenEnc, accessExpiresAt, "connected", null);
  }
}
