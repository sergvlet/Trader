// src/main/java/com/chicu/trader/trading/indicator/RsiCalculator.java
package com.chicu.trader.strategy.rsiema;

import java.util.ArrayList;
import java.util.List;

/**
 * Утилита для расчёта индекса относительной силы (RSI).
 */
public class RsiCalculator {

    /**
     * Рассчитывает список значений RSI.
     *
     * @param prices список цен закрытия
     * @param period период RSI
     * @return список RSI, начиная с точки, когда он может быть рассчитан
     */
    public static List<Double> calculate(List<Double> prices, int period) {
        List<Double> result = new ArrayList<>();
        if (prices == null || prices.size() <= period) return result;

        double gainSum = 0.0, lossSum = 0.0;

        for (int i = 1; i <= period; i++) {
            double diff = prices.get(i) - prices.get(i - 1);
            if (diff >= 0) gainSum += diff;
            else lossSum -= diff;
        }

        double avgGain = gainSum / period;
        double avgLoss = lossSum / period;

        double rs = avgLoss == 0 ? 100 : avgGain / avgLoss;
        result.add(100 - (100 / (1 + rs)));

        for (int i = period + 1; i < prices.size(); i++) {
            double diff = prices.get(i) - prices.get(i - 1);
            double gain = diff > 0 ? diff : 0;
            double loss = diff < 0 ? -diff : 0;

            avgGain = (avgGain * (period - 1) + gain) / period;
            avgLoss = (avgLoss * (period - 1) + loss) / period;

            rs = avgLoss == 0 ? 100 : avgGain / avgLoss;
            result.add(100 - (100 / (1 + rs)));
        }

        return result;
    }

    /**
     * Возвращает последнее значение RSI.
     *
     * @param prices список цен закрытия
     * @param period период RSI
     * @return последнее значение RSI, либо 50.0 по умолчанию
     */
    public static double latest(List<Double> prices, int period) {
        List<Double> rsi = calculate(prices, period);
        return rsi.isEmpty() ? 50.0 : rsi.get(rsi.size() - 1);
    }
}
