package com.example.stock_dashboard.controller;

import com.example.stock_dashboard.model.Stock;
import com.example.stock_dashboard.model.StockPriceHistory;
import com.example.stock_dashboard.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    @Autowired
    private StockService stockService;

    @GetMapping
    public List<Stock> getAllStocks() {
        return stockService.getAllStocks();
    }

    @GetMapping("/{symbol}")
    public Stock getStock(@PathVariable String symbol) {
        return stockService.getStockBySymbol(symbol);
    }

    @PostMapping("/{symbol}/update")
    public Stock updateStock(@PathVariable String symbol) {
        return stockService.updateStockPrice(symbol).join();
    }
    @GetMapping("/{symbol}/history")
    public List<StockPriceHistory> getPriceHistory(@PathVariable String symbol) {
        return stockService.getPriceHistory(symbol);
    }

    @GetMapping("/{symbol}/analytics")
    public Map<String, Object> getAnalytics(@PathVariable String symbol) {
        return stockService.getStockAnalytics(symbol);
    }

    @GetMapping("/data-source")
    public Map<String, Object> getDataSourceInfo() {
        return stockService.getDataSourceInfo();
    }
}