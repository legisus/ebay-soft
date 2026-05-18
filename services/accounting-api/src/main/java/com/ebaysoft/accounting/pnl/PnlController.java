package com.ebaysoft.accounting.pnl;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public P&L surface. Sellers' dashboards consume this — every Money field crosses the wire as
 * {@code {"amount":"123.45","currency":"USD"}} via {@code common-web}'s Jackson module.
 */
@RestController
@RequestMapping("/v1/pnl")
@RequiredArgsConstructor
public class PnlController {

  private final PnlRepository repo;

  @GetMapping
  @Operation(operationId = "getPnl", summary = "P&L rows for a tenant in [from, to] grouped by day|month|year|listing|category.")
  public List<Pnl> pnl(
      @RequestParam("tenantId") UUID tenantId,
      @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(value = "groupBy", defaultValue = "day")
          @Parameter(description = "one of: day, month, year, listing, category") String groupBy) {
    return repo.find(new PnlQuery(tenantId, from, to, groupBy));
  }
}
