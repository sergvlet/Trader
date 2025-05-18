// src/main/java/com/chicu/trader/trading/BalanceService.java
package com.chicu.trader.trading;

public interface BalanceService {
    /** Возвращает свободный USDT на аккаунте пользователя. */
    double getAvailableUsdt(Long chatId);
}
