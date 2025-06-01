// src/main/java/com/chicu/trader/trading/ml/DummyMlSignalFilter.java
package com.chicu.trader.trading.ml;

import com.chicu.trader.trading.model.MarketData;
import com.chicu.trader.trading.model.MarketSignal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Простейшая заглушка: всегда возвращает HOLD.
 */
@Slf4j
@Component
@Primary  // помечаем как @Primary, чтобы если оба бина присутствуют, брался DummyMlSignalFilter
public class DummyMlSignalFilter implements MlSignalFilter {

    @Override
    public MarketSignal predict(Long chatId, MarketData data) throws MlFilterException {
        log.debug("DummyMlSignalFilter.predict(): возвращаем HOLD");
        return MarketSignal.HOLD;
    }
}
