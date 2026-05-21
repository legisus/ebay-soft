package com.ebaysoft.accounting.sku;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/skus")
@RequiredArgsConstructor
public class SkuController {

  private final SkuRepository repo;

  @PostMapping
  public ResponseEntity<SkuResponse> upsert(
      @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantId,
      @Valid @RequestBody SkuRequest req) {
    if (tenantId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    repo.upsert(tenantId, req.skuCode(), req.title());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new SkuResponse(req.skuCode(), req.title(), Instant.now()));
  }

  @GetMapping
  public ResponseEntity<List<SkuResponse>> list(
      @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantId) {
    if (tenantId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    return ResponseEntity.ok(
        repo.findByTenant(tenantId).stream()
            .map(s -> new SkuResponse(s.skuCode(), s.title(), s.createdAt()))
            .toList());
  }

  @DeleteMapping("/{skuCode}")
  public ResponseEntity<Void> delete(
      @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantId,
      @PathVariable String skuCode) {
    if (tenantId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    int n = repo.delete(tenantId, skuCode);
    return n > 0 ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }

  public record SkuRequest(@NotBlank String skuCode, String title) {}

  public record SkuResponse(String skuCode, String title, Instant createdAt) {}
}
