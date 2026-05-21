package com.ebaysoft.accounting.cogs;

import com.ebaysoft.domain.money.Money;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manual COGS entry. POST upserts a (sku, effective_from) row; GET lists; DELETE removes a single
 * (sku, effective_from). All endpoints are scoped to the calling tenant via the X-Tenant-Id header
 * forwarded by api-gateway. See [[issue #54]].
 */
@RestController
@RequestMapping("/v1/cogs")
@RequiredArgsConstructor
public class CogsController {

  private final CogsRepository repo;

  @PostMapping
  public ResponseEntity<CogsResponse> upsert(
      @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantId,
      @Valid @RequestBody CogsUpsertRequest req) {
    if (tenantId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    Cogs row =
        new Cogs(
            tenantId,
            req.skuCode(),
            new Money(req.amount(), Currency.getInstance(req.currency())),
            req.effectiveFrom());
    repo.upsert(row);
    return ResponseEntity.status(HttpStatus.CREATED).body(CogsResponse.of(row));
  }

  @GetMapping
  public ResponseEntity<List<CogsResponse>> list(
      @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantId,
      @RequestParam(value = "sku", required = false) String skuFilter) {
    if (tenantId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    List<Cogs> rows =
        (skuFilter == null || skuFilter.isBlank())
            ? repo.findByTenant(tenantId)
            : repo.findByTenantAndSku(tenantId, skuFilter);
    return ResponseEntity.ok(rows.stream().map(CogsResponse::of).toList());
  }

  @DeleteMapping("/{skuCode}/{effectiveFrom}")
  public ResponseEntity<Void> delete(
      @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantId,
      @org.springframework.web.bind.annotation.PathVariable String skuCode,
      @org.springframework.web.bind.annotation.PathVariable LocalDate effectiveFrom) {
    if (tenantId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    int n = repo.delete(tenantId, skuCode, effectiveFrom);
    return n > 0 ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }

  public record CogsUpsertRequest(
      @NotBlank String skuCode,
      @NotNull @PositiveOrZero BigDecimal amount,
      @NotBlank String currency,
      @NotNull LocalDate effectiveFrom) {}

  public record CogsResponse(
      String skuCode, BigDecimal amount, String currency, LocalDate effectiveFrom) {
    static CogsResponse of(Cogs c) {
      return new CogsResponse(
          c.skuCode(),
          c.cost().amount(),
          c.cost().currency().getCurrencyCode(),
          c.effectiveFrom());
    }
  }
}
