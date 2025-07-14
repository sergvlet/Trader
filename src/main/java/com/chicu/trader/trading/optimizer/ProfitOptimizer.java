package com.chicu.trader.trading.optimizer;

import com.chicu.trader.trading.entity.ProfitablePair;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.service.CandleService;
import com.chicu.trader.trading.service.ProfitablePairService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfitOptimizer {

    private final CandleService candleService;
    private final ProfitablePairService profitablePairService;

    /**
     * Основной метод оптимизации TP/SL для выбранных символов.
     */
    public void optimize(Long chatId, List<String> symbols, String timeframe) {
        Duration interval = parseDuration(timeframe);

        for (String symbol : symbols) {
            List<Candle> candles = candleService.loadHistory(symbol, interval, 500);
            if (candles.size() < 100) {
                log.warn("Недостаточно свечей для анализа: {}", symbol);
                continue;
            }

            OptimizationResult bestResult = findBestTpSl(candles);

            ProfitablePair pair = ProfitablePair.builder()
                    .userChatId(chatId)
                    .symbol(symbol)
                    .takeProfitPct(bestResult.tpPct())
                    .stopLossPct(bestResult.slPct())
                    .active(true)
                    .build();

            profitablePairService.saveOrUpdate(pair);
            log.info("🚀 Оптимизация завершена для {}: TP={}%, SL={}% ", symbol, bestResult.tpPct(), bestResult.slPct());
        }
    }

    /**
     * Поиск оптимальной комбинации TP/SL.
     */
    private OptimizationResult findBestTpSl(List<Candle> candles) {
        double bestProfit = Double.NEGATIVE_INFINITY;
        double bestTp = 0.5;
        double bestSl = 0.5;

        for (double tp = 0.3; tp <= 1.5; tp += 0.1) {
            for (double sl = 0.3; sl <= 1.5; sl += 0.1) {
                double profit = simulateProfit(candles, tp, sl);
                if (profit > bestProfit) {
                    bestProfit = profit;
                    bestTp = tp;
                    bestSl = sl;
                }
            }
        }
        return new OptimizationResult(bestTp, bestSl);
    }

    /**
     * Простая симуляция доходности на исторических данных.
     */
    private double simulateProfit(List<Candle> candles, double tpPct, double slPct) {
        double balance = 100;
        for (int i = 1; i < candles.size(); i++) {
            double entry = candles.get(i - 1).getClose();
            double tp = entry * (1 + tpPct / 100.0);
            double sl = entry * (1 - slPct / 100.0);
            double next = candles.get(i).getClose();

            if (next >= tp) {
                balance *= 1 + tpPct / 100.0;
            } else if (next <= sl) {
                balance *= 1 - slPct / 100.0;
            }
        }
        return balance;
    }

    private Duration parseDuration(String timeframe) {
        if (timeframe == null || timeframe.isEmpty()) return Duration.ofMinutes(1);
        timeframe = timeframe.trim().toLowerCase();
        if (timeframe.endsWith("m")) return Duration.ofMinutes(Integer.parseInt(timeframe.replace("m", "")));
        if (timeframe.endsWith("h")) return Duration.ofHours(Integer.parseInt(timeframe.replace("h", "")));
        if (timeframe.endsWith("d")) return Duration.ofDays(Integer.parseInt(timeframe.replace("d", "")));
        return Duration.ofMinutes(1);
    }

    private record OptimizationResult(double tpPct, double slPct) {}
}
