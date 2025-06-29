package com.chicu.trader.trading.ml;



import com.chicu.trader.trading.model.Candle;

import java.util.List;

public interface DataLoader {
    /**
     * Загружает свечи по символу и таймфрейму.
     *
     * @throws MlTrainingException при ошибке загрузки
     */
    List<Candle> loadCandles(String symbol, String timeframe) throws MlTrainingException;
}
