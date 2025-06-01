// src/main/java/com/chicu/trader/trading/ml/OnnxMlSignalFilter.java
package com.chicu.trader.trading.ml;

import com.chicu.trader.trading.model.MarketData;
import com.chicu.trader.trading.model.MarketSignal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * «ONNX-фильтр» в данной реализации – просто заглушка, возвращающая HOLD.
 * Если не нужно ещё инициализировать OrtEnvironment, весь код с onnxruntime можно убрать.
 */
@Slf4j
@Component
public class OnnxMlSignalFilter implements MlSignalFilter {

    @Override
    public MarketSignal predict(Long chatId, MarketData data) throws MlFilterException {
        // Временно просто заглушка:
        log.debug("OnnxMlSignalFilter.predict() для chatId={}, data={}, возвращаем HOLD", chatId, data);
        return MarketSignal.HOLD;
    }
}
