// src/main/java/com/chicu/trader/trading/indicator/EmaCalculator.java
package com.chicu.trader.strategy.rsiema;

import java.util.ArrayList;
import java.util.List;

/**
 * Утилита для вычисления экспоненциальной скользящей средней (EMA).
 */
public class EmaCalculator {

    /**
     * Рассчитывает список значений EMA.
     *
     * @param prices список цен (например, закрытия)
     * @param period период EMA
     * @return список EMA, начиная с точки, когда EMA может быть рассчитана
     */
    public static List<Double> calculate(List<Double> prices, int period) {
        List<Double> ema = new ArrayList<>();
        if (prices == null || prices.size() < period) return ema;

        double multiplier = 2.0 / (period + 1);
        double prevEma = prices.subList(0, period).stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        ema.add(prevEma);

        for (int i = period; i < prices.size(); i++) {
            double price = prices.get(i);
            double currEma = (price - prevEma) * multiplier + prevEma;
            ema.add(currEma);
            prevEma = currEma;
        }
        return ema;
    }

    /**
     * Возвращает последнее значение EMA.
     *
     * @param prices список цен
     * @param period период EMA
     * @return последнее EMA, либо 0.0, если недостаточно данных
     */
    public static double latest(List<Double> prices, int period) {
        List<Double> ema = calculate(prices, period);
        return ema.isEmpty() ? 0.0 : ema.get(ema.size() - 1);
    }
}
