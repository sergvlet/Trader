package com.chicu.trader.trading.scanner;

import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.provider.MarketDataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketScanner {

    private final MarketDataProvider marketDataProvider;

    public List<String> scanTopSymbols(int topN, Duration timeframe) {
        log.info("🔍 Запуск сканера рынка для выбора топ-{} монет...", topN);

        List<String> allSymbols = marketDataProvider.getAllSymbols();
        Map<String, Double> volatilityMap = new HashMap<>();

        int limit = 500; // например, 500 последних свечей
        for (String symbol : allSymbols) {
            try {
                List<Candle> candles = marketDataProvider.fetchCandles(symbol, timeframe, limit);
                if (candles.size() < 50) continue;

                double volatility = calculateVolatility(candles);
                volatilityMap.put(symbol, volatility);
            } catch (Exception e) {
                log.warn("Ошибка при загрузке свечей для {}: {}", symbol, e.getMessage());
            }
        }

        return volatilityMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .toList();
    }

    private double calculateVolatility(List<Candle> candles) {
        double sum = 0;
        for (Candle candle : candles) {
            double highLow = candle.getHigh() - candle.getLow();
            sum += highLow / candle.getClose();
        }
        return sum / candles.size();
    }
}
