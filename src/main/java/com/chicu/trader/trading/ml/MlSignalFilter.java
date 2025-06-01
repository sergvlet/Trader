// src/main/java/com/chicu/trader/trading/ml/MlSignalFilter.java
package com.chicu.trader.trading.ml;

import com.chicu.trader.trading.model.MarketData;
import com.chicu.trader.trading.model.MarketSignal;

/**
 * Интерфейс ML-фильтра: принимает MarketData и возвращает сигнал (BUY/SELL/HOLD).
 */
public interface MlSignalFilter {
    /**
     * Выполнить инференс на входных данных data для пользователя chatId.
     * @return один из MarketSignal (BUY/SELL/HOLD)
     * @throws MlFilterException при ошибке внутри фильтра
     */
    MarketSignal predict(Long chatId, MarketData data) throws MlFilterException;
}
