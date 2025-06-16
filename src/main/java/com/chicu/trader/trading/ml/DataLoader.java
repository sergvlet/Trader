// src/main/java/com/chicu/trader/trading/ml/DataLoader.java
package com.chicu.trader.trading.ml;

import com.chicu.trader.trading.entity.Candle;

import java.util.List;

public interface DataLoader {
    /**
     * Загрузить исторические свечи из БД по символу и таймфрейму.
     *
     * @throws MlTrainingException при ошибке загрузки
     */
    List<Candle> loadCandles(String symbol, String timeframe) throws MlTrainingException;
}
