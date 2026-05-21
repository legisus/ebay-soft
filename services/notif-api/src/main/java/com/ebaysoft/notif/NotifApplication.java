package com.ebaysoft.notif;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Skeleton — real flows land per #66 (consumers) and #67 (email + in-app). */
@SpringBootApplication
public class NotifApplication {
  public static void main(String[] args) {
    SpringApplication.run(NotifApplication.class, args);
  }
}
