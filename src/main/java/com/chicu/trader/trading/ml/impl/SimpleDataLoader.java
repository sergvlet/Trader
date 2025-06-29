package com.chicu.trader.trading.ml.impl;


import com.chicu.trader.trading.ml.DataLoader;
import com.chicu.trader.trading.ml.MlTrainingException;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.repository.CandleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Primary
@RequiredArgsConstructor
public class SimpleDataLoader implements DataLoader {

    private final CandleRepository candleRepository;

    @Override
    public List<Candle> loadCandles(String symbol, String timeframe) throws MlTrainingException {
        try {
            return candleRepository.findBySymbolAndTimeframeOrderByTimestampAsc(symbol, timeframe);
        } catch (Exception e) {
            throw new MlTrainingException("Не удалось загрузить свечи для " 
                + symbol + "/" + timeframe, e);
        }
    }
}
