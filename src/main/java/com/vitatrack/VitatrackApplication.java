package com.vitatrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * VitaTrack Spring Boot Application.
 *
 * @EnableAsync  – required for @Async in AIServiceImpl (UC-12 bất đồng bộ)
 *                 and ActivityServiceImpl.recalcDynamicBudget (FR-08)
 * @EnableScheduling – for scheduled wearable sync every 6h (FR-07)
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class VitatrackApplication {

    public static void main(String[] args) {
        SpringApplication.run(VitatrackApplication.class, args);
    }
}
