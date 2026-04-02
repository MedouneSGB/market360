package com.market360.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.market360")
public class Market360Application {

    public static void main(String[] args) {
        SpringApplication.run(Market360Application.class, args);
    }
}
