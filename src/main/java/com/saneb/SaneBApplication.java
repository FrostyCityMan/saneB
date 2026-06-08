package com.saneb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SaneBApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaneBApplication.class, args);
    }
}
