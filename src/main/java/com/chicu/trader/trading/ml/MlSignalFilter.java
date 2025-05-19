// src/main/java/com/chicu/trader/trading/ml/MlSignalFilter.java
package com.chicu.trader.trading.ml;

import com.chicu.trader.trading.model.MarketData;
import com.chicu.trader.trading.model.MarketSignal;

/**
 * Интерфейс инференса ML-модели.
 */
public interface MlSignalFilter {
    /**
     * По входным данным выдаёт сигнал BUY или SELL.
     */
    MarketSignal predict(Long chatId, MarketData data) throws MlInferenceException;
}
