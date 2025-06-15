package com.chicu.trader.trading.provider;

import com.chicu.trader.trading.model.Candle;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

public interface MarketDataProvider {

    // Загрузка истории свечей
    List<Candle> fetchCandles(String symbol, Duration timeframe, int limit);

    // Получение всех доступных торговых пар
    List<String> getAllSymbols();

    BigDecimal getCurrentPrice(Long chatId, String symbol);
}
