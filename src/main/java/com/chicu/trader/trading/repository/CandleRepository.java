package com.chicu.trader.trading.repository;


import com.chicu.trader.trading.model.Candle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CandleRepository extends JpaRepository<Candle, Long> {
    /**
     * Возвращает все свечи для данной пары и таймфрейма,
     * отсортированные по полю timestamp (время открытия) по возрастанию.
     */
    List<Candle> findBySymbolAndTimeframeOrderByTimestampAsc(String symbol, String timeframe);
}
