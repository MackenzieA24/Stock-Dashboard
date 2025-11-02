package com.example.stock_dashboard.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@RestController
public class HelloController {

    @GetMapping("/")
    public Resource dashboard() {
        return new ClassPathResource("templates/dashboard.html");
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello from Spring Boot!";
    }
}