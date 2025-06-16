package com.chicu.trader.trading.ml.features;

import com.chicu.trader.trading.entity.Candle;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Из списка свечей строит обучающие примеры (фичи + метки).
 */
public class CandleFeatureExtractor {

    /**
     * Простая фича: скользящая средняя цены закрытия за N предыдущих баров.
     */
    public static List<FeatureVector> extractFeatures(List<Candle> candles, int window) {
        List<FeatureVector> result = new ArrayList<>();
        // сдвигаемся по списку так, чтобы хватало предыдущих window баров и одного впереди для метки
        for (int i = window; i < candles.size() - 1; i++) {
            // текущий бар и предыдущие
            List<Candle> windowCandles = candles.subList(i - window, i);
            BigDecimal ma = movingAverage(windowCandles);
            BigDecimal currClose = candles.get(i).getClose();
            BigDecimal nextClose = candles.get(i + 1).getClose();

            // фичи и метка: будет рост (1) или падение (0)
            int label = nextClose.compareTo(currClose) > 0 ? 1 : 0;
            result.add(new FeatureVector(ma.doubleValue(), currClose.doubleValue(), label));
        }
        return result;
    }

    private static BigDecimal movingAverage(List<Candle> list) {
        BigDecimal sum = list.stream()
                .map(Candle::getClose)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(list.size()), BigDecimal.ROUND_HALF_UP);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureVector {
        private double movingAverage;
        private double close;
        private int label;
    }
}
