package com.ebaysoft.ebay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EbayConnApplication {

  public static void main(String[] args) {
    SpringApplication.run(EbayConnApplication.class, args);
  }
}
