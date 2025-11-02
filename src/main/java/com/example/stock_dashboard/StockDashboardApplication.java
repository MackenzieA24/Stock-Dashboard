package com.example.stock_dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StockDashboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockDashboardApplication.class, args);

        greet();
    }
    public static void greet() {
        System.out.println("Hello World!");
    }
}

