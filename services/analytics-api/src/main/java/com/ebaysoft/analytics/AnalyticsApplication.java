package com.ebaysoft.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Analytics service — owns materialized read-models over orders/finance events (#55), serves
 * top-SKU / dead-stock / category-mix / hourly-heatmap endpoints (#56), and the SSE live-update
 * stream the dashboard subscribes to (#57). Skeleton only at this stage; real ingestion lands when
 * #49's outbox→consumer wiring ships.
 */
@SpringBootApplication
public class AnalyticsApplication {
  public static void main(String[] args) {
    SpringApplication.run(AnalyticsApplication.class, args);
  }
}
