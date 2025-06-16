// src/main/java/com/chicu/trader/trading/ml/impl/SimpleFeatureExtractor.java
package com.chicu.trader.trading.ml.impl;

import com.chicu.trader.trading.entity.Candle;
import com.chicu.trader.trading.ml.Dataset;
import com.chicu.trader.trading.ml.FeatureExtractor;
import com.chicu.trader.trading.ml.MlTrainingException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SimpleFeatureExtractor implements FeatureExtractor {

    @Override
    public Dataset extractFeatures(List<?> rawCandles) throws MlTrainingException {
        @SuppressWarnings("unchecked")
        List<Candle> candles = (List<Candle>) rawCandles;
        int n = candles.size() - 1;
        if (n <= 0) {
            throw new MlTrainingException("Недостаточно свечей для формирования признаков");
        }

        double[][] features = new double[n][2];
        double[]   labels   = new double[n];

        // найдём макс. объём для нормализации
        BigDecimal maxVol = candles.stream()
                .map(Candle::getVolume)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ONE);

        for (int i = 0; i < n; i++) {
            Candle cur = candles.get(i);
            Candle next = candles.get(i + 1);

            // признак 1: относительное изменение цены внутри свечи
            features[i][0] = cur.getClose()
                    .subtract(cur.getOpen())
                    .divide(cur.getOpen(), BigDecimal.ROUND_HALF_UP)
                    .doubleValue();

            // признак 2: нормализованный объём
            features[i][1] = cur.getVolume()
                    .divide(maxVol, BigDecimal.ROUND_HALF_UP)
                    .doubleValue();

            // метка: 1, если цена закрытия следующей свечи выше текущей
            labels[i] = next.getClose().compareTo(cur.getClose()) > 0 ? 1.0 : 0.0;
        }

        return new Dataset(features, labels);
    }
}
