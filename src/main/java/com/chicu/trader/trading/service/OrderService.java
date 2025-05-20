// src/main/java/com/chicu/trader/trading/service/OrderService.java
package com.chicu.trader.trading.service;

public interface OrderService {
    void placeMarketOrder(Long chatId, String symbol, double quantity);
    void placeOcoOrder(Long chatId, String symbol, double quantity, double stopPrice, double limitPrice);
    // сюда можно добавить другие типы ордеров
}
