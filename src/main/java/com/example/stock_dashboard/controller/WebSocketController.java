package com.example.stock_dashboard.controller;

import com.example.stock_dashboard.model.Stock;
import com.example.stock_dashboard.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Controller
@EnableScheduling
public class WebSocketController {

    @Autowired
    private StockService stockService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final List<String> trackedStocks = Arrays.asList("AAPL", "GOOGL", "MSFT", "TSLA", "AMZN", "META");
    private final AtomicInteger currentStockIndex = new AtomicInteger(0);
    private int updateCycle = 0;


    @Scheduled(fixedRate = 60000) // 1 minute
    public void sendStockUpdates() {
        updateCycle++;
        System.out.println("\n === STOCK UPDATE CYCLE " + updateCycle + " ===");
        System.out.println("Time " + LocalDateTime.now().toLocalTime());

        if (stockService.getDataSourceInfo().get("usingRealData").equals(true)) {
            updateWithRealDataStrategy();
        } else {
            updateWithSimulatedData();
        }

        List<Stock> updatedStocks = stockService.getAllStocks();
        messagingTemplate.convertAndSend("/topic/stocks", updatedStocks);

        System.out.println("Update cycle complete. Sent " + updatedStocks.size() + " stocks to clients");
        System.out.println("Next update in 60 seconds...");
    }

    private void updateWithRealDataStrategy() {
        System.out.println("Using REAL DATA strategy with Alpha Vantage API");

        String realDataStock = getNextStockForRealData();
        System.out.println("Fetching REAL data for: " + realDataStock);
        stockService.updateStockPrice(realDataStock);
        trackedStocks.stream()
                .filter(symbol -> !symbol.equals(realDataStock))
                .forEach(symbol -> {
                    System.out.println("Using SIMULATED data for: " + symbol);
                    stockService.updateStockPrice(symbol); // Will use simulated for non-primary
                });
    }

    private void updateWithSimulatedData() {
        System.out.println("Using SIMULATED DATA strategy (API not configured)");

        // Update all stocks with simulated data
        trackedStocks.forEach(symbol -> {
            stockService.updateStockPrice(symbol);
        });
    }

    private String getNextStockForRealData() {
        int index = currentStockIndex.getAndUpdate(i -> (i + 1) % trackedStocks.size());
        return trackedStocks.get(index);
    }

    @org.springframework.messaging.handler.annotation.MessageMapping("/updateStocks")
    public void updateStocks() {
        System.out.println("Manual update requested via WebSocket");
        sendStockUpdates(); // Reuse the same update logic
    }

    // Additional endpoint for manual refresh with real data
    @org.springframework.messaging.handler.annotation.MessageMapping("/refreshRealData")
    public void refreshRealData() {
        System.out.println("Manual REAL DATA refresh requested");
        if (stockService.getDataSourceInfo().get("usingRealData").equals(true)) {
            stockService.refreshAllWithRealData();
            List<Stock> updatedStocks = stockService.getAllStocks();
            messagingTemplate.convertAndSend("/topic/stocks", updatedStocks);
        }
    }
}