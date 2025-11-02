package com.example.stock_dashboard.model;

import java.time.LocalDateTime;

public class Stock {
    private String symbol;
    private double price;
    private double change;
    private double changePercent;
    private LocalDateTime lastUpdated;

    public Stock(String symbol, double price) {
        this.symbol = symbol;
        this.price = price;
        this.lastUpdated = LocalDateTime.now();
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getChange() { return change; }
    public void setChange(double change) { this.change = change; }

    public double getChangePercent() { return changePercent; }
    public void setChangePercent(double changePercent) { this.changePercent = changePercent; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}