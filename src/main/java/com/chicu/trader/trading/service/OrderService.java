// src/main/java/com/chicu/trader/trading/service/OrderService.java
package com.chicu.trader.trading.service;

import com.chicu.trader.trading.service.binance.BinanceOrderException;

public interface OrderService {
    void placeMarketOrder(Long chatId, String symbol, double quantity) throws BinanceOrderException;
    boolean placeOcoOrder(Long chatId, String symbol, double quantity, double stopPrice, double limitPrice) throws BinanceOrderException;
    // сюда можно добавить другие типы ордеров
}
