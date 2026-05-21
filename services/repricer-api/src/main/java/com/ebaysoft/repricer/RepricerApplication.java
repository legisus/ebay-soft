package com.ebaysoft.repricer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Skeleton — Phase 6 rule-based repricer + scheduled worker land per #96-#100. */
@SpringBootApplication
public class RepricerApplication {
  public static void main(String[] args) {
    SpringApplication.run(RepricerApplication.class, args);
  }
}
