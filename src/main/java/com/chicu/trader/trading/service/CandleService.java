// src/main/java/com/chicu/trader/trading/service/CandleService.java
package com.chicu.trader.trading.service;

import com.chicu.trader.trading.model.Candle;

import java.time.Duration;
import java.util.List;

public interface CandleService {
    List<Candle> history(String symbol, Duration interval, int limit);
    /**
     * Пришёл новый бар из WebSocket — можно сразу обрабатывать стратегию.
     */
    void onWebSocketCandleUpdate(Candle candle);
}
