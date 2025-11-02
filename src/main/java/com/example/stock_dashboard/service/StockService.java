package com.example.stock_dashboard.service;

import com.example.stock_dashboard.model.Stock;
import com.example.stock_dashboard.repo.StockHistoryRepository;
import com.example.stock_dashboard.model.StockPriceHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@EnableAsync
public class StockService {

    @Autowired
    private StockHistoryRepository historyRepository;

    @Autowired
    private AlphaVantageService alphaVantageService;

    private final Map<String, Stock> stockCache = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private boolean useRealData = false;

    @PostConstruct
    public void initialize() {
        System.out.println("Initializing StockService...");

        useRealData = alphaVantageService.isApiKeyConfigured();
        if (useRealData) {
            System.out.println("Real Alpha Vantage API configured - using live market data");
        } else {
            System.out.println("Using simulated data - configure Alpha Vantage API key for real market data");
        }

        initializeSampleStocks();
    }

    private void initializeSampleStocks() {
        String[] symbols = {"AAPL", "GOOGL", "MSFT", "TSLA", "AMZN", "META"};

        for (String symbol : symbols) {
            if (useRealData) {
                Stock realStock = alphaVantageService.fetchRealTimeStockData(symbol);
                if (realStock != null) {
                    stockCache.put(symbol, realStock);
                    savePriceHistory(symbol, realStock.getPrice());
                    System.out.println("Initialized " + symbol + " with real data: $" + realStock.getPrice());
                    continue;
                } else {
                    System.out.println("Failed to fetch real data for " + symbol + ", using simulated data");
                }
            }

            // Fallback to simulated data
            double simulatedPrice = getSimulatedPrice(symbol);
            Stock simulatedStock = new Stock(symbol, simulatedPrice);
            stockCache.put(symbol, simulatedStock);
            savePriceHistory(symbol, simulatedPrice);
            System.out.println("Initialized " + symbol + " with simulated data: $" + simulatedPrice);
        }

        System.out.println("StockService initialization complete");
        System.out.println("Tracking " + stockCache.size() + " stocks");
        System.out.println("Data source: " + (useRealData ? "Alpha Vantage API" : "Simulated Data"));
    }

    private double getSimulatedPrice(String symbol) {
        // Different base prices for different symbols for realism
        Map<String, Double> basePrices = Map.of(
                "AAPL", 150.0, "GOOGL", 2700.0, "MSFT", 300.0,
                "TSLA", 200.0, "AMZN", 3400.0, "META", 320.0
        );
        return basePrices.getOrDefault(symbol, 100.0);
    }

    public List<Stock> getAllStocks() {
        return new ArrayList<>(stockCache.values());
    }

    public Stock getStockBySymbol(String symbol) {
        return stockCache.get(symbol.toUpperCase());
    }

    @Async
    public CompletableFuture<Stock> updateStockPrice(String symbol) {
        Stock stock = stockCache.get(symbol);
        if (stock == null) {
            return CompletableFuture.completedFuture(null);
        }

        if (useRealData) {
            // Try to get real data from Alpha Vantage
            Stock realStock = alphaVantageService.fetchRealTimeStockData(symbol);
            if (realStock != null) {
                // Update the existing stock object with real data
                stock.setPrice(realStock.getPrice());
                stock.setChange(realStock.getChange());
                stock.setChangePercent(realStock.getChangePercent());
                stock.setLastUpdated(LocalDateTime.now());
                savePriceHistory(symbol, realStock.getPrice());
                System.out.println("Real data update: " + symbol + " = $" + realStock.getPrice() +
                        " (" + realStock.getChangePercent() + "%)");
                return CompletableFuture.completedFuture(stock);
            } else {
                System.out.println("Real data fetch failed for " + symbol + ", using simulated update");
            }
        }

        double oldPrice = stock.getPrice();
        // Generate random price change between -2% and +2%
        double changeFactor = 0.96 + (random.nextDouble() * 0.08);
        double newPrice = Math.round(oldPrice * changeFactor * 100.0) / 100.0;

        stock.setPrice(newPrice);
        stock.setChange(newPrice - oldPrice);
        stock.setChangePercent(((newPrice - oldPrice) / oldPrice) * 100);
        stock.setLastUpdated(LocalDateTime.now());
        savePriceHistory(symbol, newPrice);
        System.out.println("Simulated update: " + symbol + " = $" + newPrice +
                " (" + stock.getChangePercent() + "%)");

        return CompletableFuture.completedFuture(stock);
    }

    private void savePriceHistory(String symbol, double price) {
        try {
            StockPriceHistory history = new StockPriceHistory(symbol, price);
            historyRepository.save(history);
        } catch (Exception e) {
            System.err.println("Error saving price history for " + symbol + ": " + e.getMessage());
        }
    }

    // Update all stock prices at once
    public void updateAllStockPrices() {
        stockCache.keySet().forEach(symbol -> {
            updateStockPrice(symbol);
        });
    }

    public List<StockPriceHistory> getPriceHistory(String symbol) {
        try {
            return historyRepository.findTop10BySymbolOrderByTimestampDesc(symbol);
        } catch (Exception e) {
            System.err.println("Error fetching price history for " + symbol + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Map<String, Object> getStockAnalytics(String symbol) {
        try {
            List<StockPriceHistory> history = historyRepository.findBySymbolOrderByTimestampDesc(symbol);

            if (history.isEmpty()) {
                return Map.of("error", "No historical data available for " + symbol);
            }

            double currentPrice = history.get(0).getPrice();
            List<Double> prices = history.stream()
                    .map(StockPriceHistory::getPrice)
                    .toList();

            double minPrice = prices.stream().mapToDouble(Double::doubleValue).min().orElse(currentPrice);
            double maxPrice = prices.stream().mapToDouble(Double::doubleValue).max().orElse(currentPrice);
            double avgPrice = prices.stream().mapToDouble(Double::doubleValue).average().orElse(currentPrice);

            double trend = currentPrice - history.get(history.size() - 1).getPrice();
            String trendDirection = trend >= 0 ? "up" : "down";

            return Map.of(
                    "symbol", symbol,
                    "currentPrice", currentPrice,
                    "minPrice", Math.round(minPrice * 100.0) / 100.0,
                    "maxPrice", Math.round(maxPrice * 100.0) / 100.0,
                    "avgPrice", Math.round(avgPrice * 100.0) / 100.0,
                    "dataPoints", history.size(),
                    "trend", Math.round(trend * 100.0) / 100.0,
                    "trendDirection", trendDirection,
                    "priceChange", Math.round(((currentPrice - history.get(history.size() - 1).getPrice()) / history.get(history.size() - 1).getPrice() * 100) * 100.0) / 100.0 + "%",
                    "dataSource", useRealData ? "Alpha Vantage API" : "Simulated Data"
            );
        } catch (Exception e) {
            System.err.println("Error calculating analytics for " + symbol + ": " + e.getMessage());
            return Map.of("error", "Failed to calculate analytics for " + symbol);
        }
    }

    public Map<String, Map<String, Object>> getAllStocksAnalytics() {
        Map<String, Map<String, Object>> allAnalytics = new HashMap<>();
        stockCache.keySet().forEach(symbol -> {
            allAnalytics.put(symbol, getStockAnalytics(symbol));
        });
        return allAnalytics;
    }

    public List<StockPriceHistory> getRecentPriceHistory(String symbol, int limit) {
        try {
            return historyRepository.findBySymbolOrderByTimestampDesc(symbol)
                    .stream()
                    .limit(limit)
                    .toList();
        } catch (Exception e) {
            System.err.println("Error fetching recent history for " + symbol + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Map<String, Object> getDatabaseStats() {
        try {
            long totalRecords = historyRepository.count();
            long uniqueStocks = historyRepository.findAll().stream()
                    .map(StockPriceHistory::getSymbol)
                    .distinct()
                    .count();

            Optional<StockPriceHistory> oldestRecord = historyRepository.findAll().stream()
                    .min(Comparator.comparing(StockPriceHistory::getTimestamp));

            Optional<StockPriceHistory> newestRecord = historyRepository.findAll().stream()
                    .max(Comparator.comparing(StockPriceHistory::getTimestamp));

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalRecords", totalRecords);
            stats.put("uniqueStocks", uniqueStocks);
            stats.put("oldestRecord", oldestRecord.map(StockPriceHistory::getTimestamp).orElse(null));
            stats.put("newestRecord", newestRecord.map(StockPriceHistory::getTimestamp).orElse(null));
            stats.put("dataSource", useRealData ? "Alpha Vantage API" : "Simulated Data");

            return stats;
        } catch (Exception e) {
            System.err.println("Error fetching database stats: " + e.getMessage());
            return Map.of("error", "Failed to fetch database statistics");
        }
    }

    // Get information about data source
    public Map<String, Object> getDataSourceInfo() {
        return Map.of(
                "usingRealData", useRealData,
                "apiConfigured", alphaVantageService.isApiKeyConfigured(),
                "trackedStocks", stockCache.size(),
                "stocks", String.join(", ", stockCache.keySet()),
                "message", useRealData ?
                        "Connected to Alpha Vantage API - Live market data" :
                        alphaVantageService.isApiKeyConfigured() ?
                                "Alpha Vantage API configured but rate limited - Using simulated data" :
                                "Using simulated data - Configure API key for live market data"
        );
    }

    public void refreshAllWithRealData() {
        if (!useRealData) {
            System.out.println("Cannot refresh with real data - API not configured");
            return;
        }

        System.out.println("Manually refreshing all stocks with real data...");
        stockCache.keySet().forEach(symbol -> {
            Stock realStock = alphaVantageService.fetchRealTimeStockData(symbol);
            if (realStock != null) {
                Stock existingStock = stockCache.get(symbol);
                existingStock.setPrice(realStock.getPrice());
                existingStock.setChange(realStock.getChange());
                existingStock.setChangePercent(realStock.getChangePercent());
                existingStock.setLastUpdated(LocalDateTime.now());
                savePriceHistory(symbol, realStock.getPrice());
                System.out.println("Refreshed " + symbol + " with real data: $" + realStock.getPrice());
            }
        });
    }
}