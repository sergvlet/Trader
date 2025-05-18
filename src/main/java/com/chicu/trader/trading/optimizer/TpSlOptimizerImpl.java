package com.chicu.trader.trading.optimizer;

import com.chicu.trader.trading.model.Candle;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Простая реализация TpSlOptimizer: перебирает TP% от 3% до 6%, SL% от 1% до 2%
 * и выбирает пару значений с максимальной суммарной прибыли на заданной истории.
 */
@Service
public class TpSlOptimizerImpl implements TpSlOptimizer {

    @Override
    public Result optimize(List<Candle> history) {
        double bestProfit = Double.NEGATIVE_INFINITY;
        double bestTp = 0.03;
        double bestSl = 0.01;

        // берём цену входа как цена первого элемента истории
        double entryPrice = history.get(0).getClose();

        // простой перебор: TP от 3% до 6%, SL от 1% до 2% с шагом 0.5%
        for (double tp = 0.03; tp <= 0.06; tp += 0.005) {
            for (double sl = 0.01; sl <= 0.02; sl += 0.005) {
                double tpPrice = entryPrice * (1 + tp);
                double slPrice = entryPrice * (1 - sl);
                double profit = 0;

                // проходим по истории и считаем PnL, если за кадром
                for (Candle c : history) {
                    double high = c.getHigh(), low = c.getLow();
                    if (high >= tpPrice) {
                        profit += tp;  // достигли TP
                    } else if (low <= slPrice) {
                        profit -= sl;  // достигли SL
                    }
                }

                if (profit > bestProfit) {
                    bestProfit = profit;
                    bestTp = tp;
                    bestSl = sl;
                }
            }
        }

        return new Result(bestTp, bestSl, bestProfit);
    }
}
