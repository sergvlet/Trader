package com.chicu.trader.trading.ml;

import com.chicu.trader.trading.entity.Candle;

import java.util.List;

public class DatasetBuilder {
    public static Dataset from(List<Candle> candles) {
        // Пример: скользящий featurizer
        int n = candles.size() - 1;
        double[][] X = new double[n][4];  // например open/high/low/close
        double[]   y = new double[n];
        for (int i = 0; i < n; i++) {
            Candle c0 = candles.get(i);
            Candle c1 = candles.get(i + 1);
            X[i][0] = c0.getOpen().doubleValue();
            X[i][1] = c0.getHigh().doubleValue();
            X[i][2] = c0.getLow().doubleValue();
            X[i][3] = c0.getClose().doubleValue();
            y[i]    = c1.getClose().doubleValue() > c0.getClose().doubleValue() ? 1.0 : 0.0;
        }
        return new Dataset(X, y);
    }
}
