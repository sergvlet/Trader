// src/main/java/com/chicu/trader/trading/PositionService.java
package com.chicu.trader.trading;

public interface PositionService {
    /** Максимально возможных слотов (константа). */
    int MAX_SLOTS = 5;

    /** Сколько слотов занято (открытых позиций) у пользователя. */
    int getActiveSlots(Long chatId);

    /** Сколько базового актива по символу открыто у пользователя. */
    double getOpenPositionQuantity(Long chatId, String symbol);
}
