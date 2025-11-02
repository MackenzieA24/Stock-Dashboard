package com.example.stock_dashboard.service;

import com.example.stock_dashboard.model.Stock;
import com.example.stock_dashboard.config.AlphaVantageConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlphaVantageService {

    @Autowired
    private AlphaVantageConfig config;

    @Autowired
    private RestTemplate restTemplate;

    private final Map<String, Long> lastApiCall = new ConcurrentHashMap<>();
    private static final long API_CALL_INTERVAL = 15000; // 15 seconds between calls

    public Stock fetchRealTimeStockData(String symbol) {
        if (!canMakeApiCall(symbol)) {
            System.out.println("Rate limit reached for " + symbol + ", using cached data");
            return null;
        }

        try {
            String url = buildApiUrl("GLOBAL_QUOTE", symbol);
            System.out.println("Fetching real data for " + symbol + " from Alpha Vantage...");

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("Global Quote")) {
                Map<String, String> quote = (Map<String, String>) responseBody.get("Global Quote");
                return parseStockData(symbol, quote);
            } else if (responseBody != null && responseBody.containsKey("Note")) {
                System.out.println("API rate limit note: " + responseBody.get("Note"));
                return null;
            } else if (responseBody != null && responseBody.containsKey("Error Message")) {
                System.err.println("API Error: " + responseBody.get("Error Message"));
                return null;
            }

        } catch (HttpClientErrorException.TooManyRequests e) {
            System.err.println("Alpha Vantage rate limit exceeded for " + symbol);
        } catch (HttpClientErrorException e) {
            System.err.println("HTTP Error fetching " + symbol + ": " + e.getStatusCode());
        } catch (ResourceAccessException e) {
            System.err.println("Network Error fetching " + symbol + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error fetching " + symbol + ": " + e.getMessage());
        }

        return null;
    }

    private boolean canMakeApiCall(String symbol) {
        Long lastCall = lastApiCall.get(symbol);
        long currentTime = System.currentTimeMillis();

        if (lastCall == null || (currentTime - lastCall) > API_CALL_INTERVAL) {
            lastApiCall.put(symbol, currentTime);
            return true;
        }
        return false;
    }

    private String buildApiUrl(String function, String symbol) {
        return String.format("%s?function=%s&symbol=%s&apikey=%s",
                config.getApiUrl(), function, symbol, config.getApiKey());
    }

    private Stock parseStockData(String symbol, Map<String, String> quote) {
        try {
            double price = Double.parseDouble(quote.get("05. price"));
            double change = Double.parseDouble(quote.get("09. change"));
            double changePercent = Double.parseDouble(quote.get("10. change percent").replace("%", ""));

            Stock stock = new Stock(symbol, price);
            stock.setChange(change);
            stock.setChangePercent(changePercent);

            System.out.println("Successfully fetched real data: " + symbol + " = $" + price);
            return stock;

        } catch (Exception e) {
            System.err.println("Error parsing stock data for " + symbol + ": " + e.getMessage());
            return null;
        }
    }

    public boolean isApiKeyConfigured() {
        return !"demo".equals(config.getApiKey()) && config.getApiKey() != null
                && !config.getApiKey().trim().isEmpty();
    }
}