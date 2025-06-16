package com.chicu.trader.trading.ml;

import java.util.List;

/**
 * Из свечей формирует набор признаков и меток.
 */
public interface FeatureExtractor {
    /**
     * Превращает список свечей в матрицу признаков и метки.
     * @param candles исторические свечи
     * @return Dataset
     */
    Dataset extractFeatures(List<?> candles) throws MlTrainingException;
}
