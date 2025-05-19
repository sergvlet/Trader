// src/main/java/com/chicu/trader/trading/service/OrderService.java
package com.chicu.trader.trading.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Сервис отправки ордеров на биржу.
 */
@Service
@Slf4j
public class OrderService {

    /**
     * Размещает OCO-ордер (One-Cancels-the-Other) с тейк-профитом и стоп-лоссом.
     *
     * @param chatId         чат пользователя
     * @param symbol         торговая пара
     * @param quantity       количество актива
     * @param stopLossPrice  цена стоп-лосса
     * @param takeProfitPrice цена тейк-профита
     */
    public void placeOcoOrder(Long chatId, String symbol, double quantity,
                              double stopLossPrice, double takeProfitPrice) {
        log.info("OCO-ордер: chatId={} symbol={} qty={} SL={} TP={}",
                 chatId, symbol, quantity, stopLossPrice, takeProfitPrice);
        // TODO: вызвать API биржи через NetworkSettingsService
    }

    /**
     * Размещает рыночный ордер для выхода из позиции.
     *
     * @param chatId   чат пользователя
     * @param symbol   торговая пара
     * @param quantity количество актива
     */
    public void placeMarketOrder(Long chatId, String symbol, double quantity) {
        log.info("Market-ордер: chatId={} symbol={} qty={}", chatId, symbol, quantity);
        // TODO: вызвать API биржи через NetworkSettingsService
    }
}
