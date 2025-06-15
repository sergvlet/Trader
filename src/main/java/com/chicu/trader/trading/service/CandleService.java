package com.chicu.trader.trading.service;

import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.provider.MarketDataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Единая точка доступа к свечам.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CandleService {

    private final MarketDataProvider marketDataProvider;

    // Кэш последних свечей на случай частых вызовов
    private final ConcurrentHashMap<String, List<Candle>> cache = new ConcurrentHashMap<>();

    /**
     * Загружаем историю свечей с Binance (или другого провайдера)
     */
    public List<Candle> loadHistory(String symbol, Duration timeframe, int limit) {
        String cacheKey = symbol + ":" + timeframe.toMinutes() + ":" + limit;
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        List<Candle> candles = marketDataProvider.fetchCandles(symbol, timeframe, limit);
        cache.put(cacheKey, candles);
        return candles;
    }

    /**
     * Принудительно обновляем кэш (напр. по таймеру)
     */
    public void refreshCache(String symbol, Duration timeframe, int limit) {
        String cacheKey = symbol + ":" + timeframe.toMinutes() + ":" + limit;
        List<Candle> candles = marketDataProvider.fetchCandles(symbol, timeframe, limit);
        cache.put(cacheKey, candles);
    }

    /**
     * Очистка всего кэша (если потребуется)
     */
    public void clearCache() {
        cache.clear();
    }
    public double getCurrentPrice(String symbol) {
        List<Candle> candles = cache.get(symbol);  // если ты держишь кэш
        if (candles != null && !candles.isEmpty()) {
            return candles.get(candles.size() - 1).getClose();
        }
        throw new IllegalStateException("No cached candles for symbol: " + symbol);
    }

}
