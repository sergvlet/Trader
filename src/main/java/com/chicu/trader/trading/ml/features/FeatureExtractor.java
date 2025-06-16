package com.chicu.trader.trading.ml.features;

import com.chicu.trader.trading.entity.Candle;
import com.chicu.trader.trading.ml.MlTrainingException;

import java.util.List;

public interface FeatureExtractor {
    double[] extract(List<Candle> window) throws MlTrainingException;
}
