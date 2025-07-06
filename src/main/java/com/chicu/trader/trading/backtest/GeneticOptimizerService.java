package com.chicu.trader.trading.backtest;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.strategy.StrategyRegistry;
import com.chicu.trader.strategy.StrategySettings;
import com.chicu.trader.trading.backtest.service.BacktestService;
import com.chicu.trader.trading.backtest.service.BacktestSettingsService;
import com.chicu.trader.trading.model.BacktestSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class GeneticOptimizerService {
    private final AiTradingSettingsService   aiSettingsService;
    private final BacktestSettingsService    btSettingsService;
    private final BacktestService            backtestService;
    private final StrategyRegistry           strategyRegistry;

    /**
     * Синхронно прогоняет эволюционный алгоритм, сохраняет лучшие настройки
     * и возвращает итоговую сводку по найденному PnL.
     */
    public String optimizeEvolutionarySync(Long chatId) {
        AiTradingSettings ai       = aiSettingsService.getSettingsOrThrow(chatId);
        BacktestSettings  original = btSettingsService.getOrCreate(chatId);
        StrategySettings  strat    = strategyRegistry.getSettings(ai.getStrategy(), chatId);

        double[] commissions = {0.1, 0.2, 0.3, 0.4, 0.5};
        int[]    limits      = {100, 250, 500};
        String[] tfs         = {"1m", "5m", "15m"};

        int    populationSize = 10;
        int    generations    = 5;
        double mutationRate   = 0.3;

        List<Candidate> population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            population.add(randomCandidate(commissions, limits, tfs));
        }

        Candidate best    = null;
        double    bestPnl = Double.NEGATIVE_INFINITY;

        for (int gen = 0; gen < generations; gen++) {
            for (Candidate c : population) {
                // записываем параметры и запускаем бэктест
                btSettingsService.save(c.toBacktestSettings(chatId, original));
                double pnl = backtestService.runBacktest(chatId).getTotalPnl();
                c.fitness = pnl;
                if (pnl > bestPnl) {
                    bestPnl = pnl;
                    best    = c;
                }
                // при первом положительном PnL сразу сохраняем и возвращаем результат
                if (pnl > 0) {
                    applyBest(chatId, best);
                    return formatSummary(bestPnl, best);
                }
            }
            // селекция по лучшим половинам
            population.sort(Comparator.comparingDouble(Candidate::getFitness).reversed());
            List<Candidate> elites = population.subList(0, populationSize / 2);
            List<Candidate> nextGen = new ArrayList<>(elites);
            // кроссовер + возможная мутация
            while (nextGen.size() < populationSize) {
                Candidate p1 = elites.get(ThreadLocalRandom.current().nextInt(elites.size()));
                Candidate p2 = elites.get(ThreadLocalRandom.current().nextInt(elites.size()));
                Candidate child = crossover(p1, p2);
                if (ThreadLocalRandom.current().nextDouble() < mutationRate) {
                    child = mutate(child, commissions, limits, tfs);
                }
                nextGen.add(child);
            }
            population = nextGen;
        }

        // если за все поколения не было плюса, применяем лучший найденный
        if (best != null) {
            applyBest(chatId, best);
            return formatSummary(bestPnl, best);
        } else {
            return "Эволюция не нашла улучшений.";
        }
    }

    /**
     * Асинхронный вариант (через optimizerExecutor), оставляем на всякий случай.
     */
    @Async("optimizerExecutor")
    public void optimizeEvolutionary(Long chatId) {
        // просто вызывает синхронный и игнорирует строку-результат
        optimizeEvolutionarySync(chatId);
    }

    private void applyBest(Long chatId, Candidate best) {
        btSettingsService.save(
                best.toBacktestSettings(chatId, btSettingsService.getOrCreate(chatId))
        );
    }

    private String formatSummary(double pnl, Candidate best) {
        return String.format(
                "🧬 Эволюция завершена: PnL=%.2f%%, commission=%.1f%%, candlesLimit=%d, timeframe=%s",
                pnl * 100, best.commission, best.limit, best.timeframe
        );
    }

    private Candidate randomCandidate(double[] comms, int[] limits, String[] tfs) {
        return new Candidate(
                comms[ThreadLocalRandom.current().nextInt(comms.length)],
                limits[ThreadLocalRandom.current().nextInt(limits.length)],
                tfs[ThreadLocalRandom.current().nextInt(tfs.length)]
        );
    }

    private Candidate crossover(Candidate a, Candidate b) {
        return new Candidate(
                ThreadLocalRandom.current().nextBoolean() ? a.commission : b.commission,
                ThreadLocalRandom.current().nextBoolean() ? a.limit      : b.limit,
                ThreadLocalRandom.current().nextBoolean() ? a.timeframe  : b.timeframe
        );
    }

    private Candidate mutate(Candidate c, double[] comms, int[] limits, String[] tfs) {
        return new Candidate(
                comms[ThreadLocalRandom.current().nextInt(comms.length)],
                limits[ThreadLocalRandom.current().nextInt(limits.length)],
                tfs[ThreadLocalRandom.current().nextInt(tfs.length)]
        );
    }

    private static class Candidate {
        final double commission;
        final int    limit;
        final String timeframe;
        double      fitness;

        Candidate(double commission, int limit, String timeframe) {
            this.commission = commission;
            this.limit      = limit;
            this.timeframe  = timeframe;
        }

        double getFitness() {
            return fitness;
        }

        BacktestSettings toBacktestSettings(Long chatId, BacktestSettings base) {
            return BacktestSettings.builder()
                    .chatId(chatId)
                    .startDate(base.getStartDate())
                    .endDate(base.getEndDate())
                    .commissionPct(commission)
                    .cachedCandlesLimit(limit)
                    .timeframe(timeframe)
                    .leverage(base.getLeverage())
                    .build();
        }
    }
}
