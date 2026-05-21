package com.ebaysoft.accounting.exports;

import com.ebaysoft.accounting.pnl.Pnl;
import com.ebaysoft.accounting.pnl.PnlQuery;
import com.ebaysoft.accounting.pnl.PnlRepository;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * CSV export of P&amp;L rows. XLSX + PDF + signed MinIO URLs are deferred per the tight scope on
 * issue #53 — once MinIO is wired and we pick a PDF lib, the same controller grows
 * {@code /pnl.xlsx} and {@code /pnl.pdf} siblings + the redirect-to-signed-URL flow.
 *
 * <p>Streams the body directly so a multi-year export doesn't materialize in memory.
 */
@RestController
@RequestMapping("/v1/exports")
@RequiredArgsConstructor
public class ExportController {

  private static final String[] HEADERS = {
    "date", "group_by", "group_key", "currency",
    "revenue", "fees", "refunds", "cogs", "shipping", "ads", "net"
  };

  private final PnlRepository repo;

  @GetMapping(value = "/pnl.csv", produces = "text/csv;charset=UTF-8")
  public ResponseEntity<StreamingResponseBody> pnlCsv(
      @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantId,
      @RequestParam("from") LocalDate from,
      @RequestParam("to") LocalDate to,
      @RequestParam(value = "groupBy", defaultValue = "day") String groupBy) {
    if (tenantId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

    String filename = "pnl-%s-%s.csv".formatted(from, to);

    StreamingResponseBody body =
        outputStream -> {
          try (Writer w = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            writeHeader(w);
            for (Pnl row : repo.find(new PnlQuery(tenantId, from, to, groupBy))) {
              writeRow(w, row);
            }
            w.flush();
          }
        };

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
        .body(body);
  }

  private static void writeHeader(Writer w) throws IOException {
    for (int i = 0; i < HEADERS.length; i++) {
      if (i > 0) w.write(',');
      w.write(HEADERS[i]);
    }
    w.write('\n');
  }

  private static void writeRow(Writer w, Pnl r) throws IOException {
    String currency = r.revenue().currency().getCurrencyCode();
    String[] cols = {
      r.date().toString(),
      r.groupBy(),
      r.groupKey(),
      currency,
      r.revenue().amount().toPlainString(),
      r.fees().amount().toPlainString(),
      r.refunds().amount().toPlainString(),
      r.cogs().amount().toPlainString(),
      r.shipping().amount().toPlainString(),
      r.ads().amount().toPlainString(),
      r.net().amount().toPlainString()
    };
    for (int i = 0; i < cols.length; i++) {
      if (i > 0) w.write(',');
      w.write(escape(cols[i]));
    }
    w.write('\n');
  }

  /** RFC 4180-style escape: double quotes when the field contains , " or newline. */
  static String escape(String s) {
    if (s == null) return "";
    boolean needsQuote = s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0;
    if (!needsQuote) return s;
    return "\"" + s.replace("\"", "\"\"") + "\"";
  }
}
