package com.example.stock_dashboard.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AlphaVantageConfig {

    @Value("${alpha.vantage.api.key:demo}")  // Default to 'demo' if not set
    private String apiKey;

    @Value("${alpha.vantage.api.url:https://www.alphavantage.co/query}")
    private String apiUrl;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }
}