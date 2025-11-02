package com.example.stock_dashboard.repo;

import com.example.stock_dashboard.model.StockPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface StockHistoryRepository extends JpaRepository<StockPriceHistory, Long> {

    List<StockPriceHistory> findBySymbolOrderByTimestampDesc(String symbol);

    @Query("SELECT sph FROM StockPriceHistory sph WHERE sph.symbol = :symbol ORDER BY sph.timestamp DESC")
    List<StockPriceHistory> findRecentBySymbol(String symbol, org.springframework.data.domain.Pageable pageable);

    List<StockPriceHistory> findTop10BySymbolOrderByTimestampDesc(String symbol);
}