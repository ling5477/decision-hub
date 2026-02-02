package com.guidinglight.decisionhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.guidinglight.decisionhub")
public class DecisionHubApplication {
  public static void main(String[] args) {
    SpringApplication.run(DecisionHubApplication.class, args);
  }
}
