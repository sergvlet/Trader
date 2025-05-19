// src/main/java/com/chicu/trader/trading/service/CandleService.java
package com.chicu.trader.trading.service;

import com.chicu.trader.trading.model.Candle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Сервис получения исторических свечных данных.
 */
@Service
@Slf4j
public class CandleService {

    /**
     * Исторические часовые свечи.
     *
     * @param chatId  чат пользователя
     * @param symbol  торговая пара (например, "BTCUSDT")
     * @param limit   количество свечей
     * @return список свечей
     */
    public List<Candle> historyHourly(Long chatId, String symbol, int limit) {
        log.debug("Запрос historyHourly для chatId={} symbol={} limit={}", chatId, symbol, limit);
        // TODO: заменить на реальный вызов к MarketDataService или API биржи
        return Collections.emptyList();
    }

    /**
     * Исторические 4-часовые свечи.
     *
     * @param chatId  чат пользователя
     * @param symbol  торговая пара
     * @param limit   количество свечей
     * @return список свечей
     */
    public List<Candle> history4h(Long chatId, String symbol, int limit) {
        log.debug("Запрос history4h для chatId={} symbol={} limit={}", chatId, symbol, limit);
        // TODO: заменить на реальный вызов к MarketDataService или API биржи
        return Collections.emptyList();
    }
}
