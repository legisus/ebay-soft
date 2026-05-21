package com.ebaysoft.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Inventory service — takes over the SKU master from accounting-api on the Phase-3 cutover (#60),
 * owns multi-warehouse stock_levels (#61), runs the low-stock worker that emits stock.low events
 * (#62), serves dead-stock reports (#63), and handles bulk CSV import/export (#64). Skeleton only;
 * real schema + endpoints land per-issue.
 */
@SpringBootApplication
public class InventoryApplication {
  public static void main(String[] args) {
    SpringApplication.run(InventoryApplication.class, args);
  }
}
