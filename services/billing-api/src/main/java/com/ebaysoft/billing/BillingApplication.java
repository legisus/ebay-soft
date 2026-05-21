package com.ebaysoft.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Skeleton — real flows land per #70 (Checkout), #71 (webhooks), #72 (plan limits), #73 (trial), #74 (Stripe Tax). */
@SpringBootApplication
public class BillingApplication {
  public static void main(String[] args) {
    SpringApplication.run(BillingApplication.class, args);
  }
}
