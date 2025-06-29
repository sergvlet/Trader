package com.chicu.trader.trading.ml.impl;

import com.chicu.trader.trading.ml.features.FeatureExtractor;
import com.chicu.trader.trading.model.Candle;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

@Component
public class SimpleFeatureExtractor implements FeatureExtractor {

    /**
     * Для каждого окна свечей возвращаем признаки:
     * [0] — относительный рост между последней и первой свечой
     * [1] — средний объем
     * [2] — волатильность (средний (high-low))
     */
    @Override
    public double[] extract(List<Candle> candles) {
        int n = candles.size();

        // 1) относительный рост: (close_last / open_first) - 1
        BigDecimal openFirst = BigDecimal.valueOf(candles.get(0).getOpen());
        BigDecimal closeLast = BigDecimal.valueOf(candles.get(n - 1).getClose());
        BigDecimal relGrowth = closeLast
                .divide(openFirst, 8, RoundingMode.HALF_UP)
                .subtract(BigDecimal.ONE);

        // 2) средний объем
        BigDecimal sumVolume = candles.stream()
                .map(c -> BigDecimal.valueOf(c.getVolume()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgVolume = sumVolume
                .divide(BigDecimal.valueOf(n), 8, RoundingMode.HALF_UP);

        // 3) средняя разница high-low
        BigDecimal sumHL = candles.stream()
                .map(c -> BigDecimal.valueOf(c.getHigh())
                        .subtract(BigDecimal.valueOf(c.getLow())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgHL = sumHL
                .divide(BigDecimal.valueOf(n), 8, RoundingMode.HALF_UP);

        return new double[] {
            relGrowth.doubleValue(),
            avgVolume.doubleValue(),
            avgHL.doubleValue()
        };
    }
}
