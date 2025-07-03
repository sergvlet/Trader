package com.chicu.trader.strategy.rsiema;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.ml.MlSignalFilterService;
import com.chicu.trader.ml.MlTrainingService;
import com.chicu.trader.strategy.retrain.StrategyRetrainer;
import com.chicu.trader.strategy.rsiema.model.RsiEmaStrategySettings;
import com.chicu.trader.strategy.rsiema.service.RsiEmaStrategySettingsService;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.service.CandleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RsiEmaRetrainer implements StrategyRetrainer {

    private final CandleService candleService;
    private final RsiEmaStrategySettingsService settingsService;
    private final MlSignalFilterService mlSignalFilter;
    private final MlTrainingService mlTrainingService;

    private final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );

    @Override
    public boolean retrain(AiTradingSettings settings) {
        Long chatId = settings.getChatId();

        boolean trained = mlTrainingService.runTraining();
        if (!trained) {
            log.warn("⚠️ Обучение модели завершилось с ошибкой.");
        }

        RsiEmaStrategySettings strategySettings = settingsService.getOrCreate(chatId);
        List<String> symbols = Arrays.stream(settings.getSymbols().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        boolean updated = false;

        for (String symbol : symbols) {
            try {
                int candleCount = strategySettings.getTakeProfitWindow() + 100;
                List<Candle> candles = candleService.loadHistory(symbol, Duration.ofHours(1), candleCount);
                if (candles.size() < 100) continue;

                RsiEmaStrategySettings best = findBestParams(chatId, candles, strategySettings);
                best.setChatId(chatId);
                best.setSymbol(symbol);
                best.setTimeframe("1h");
                best.setCachedCandlesLimit(candleCount);
                settingsService.save(best);
                updated = true;

                log.info("✅ Переобучение завершено: chatId={}, symbol={}, лучшая конфигурация={}", chatId, symbol, best);
            } catch (Exception e) {
                log.error("❌ Ошибка переобучения: chatId={}, symbol={}", chatId, symbol, e);
            }
        }

        return updated;
    }

    private RsiEmaStrategySettings findBestParams(Long chatId, List<Candle> candles, RsiEmaStrategySettings config) {
        List<Future<ParamResult>> futures = new ArrayList<>();

        for (int rsiPeriod : config.getRsiPeriods()) {
            for (int emaShort : config.getEmaShorts()) {
                for (int emaLong : config.getEmaLongs()) {
                    for (double buyTh : config.getRsiBuyThresholds()) {
                        for (double sellTh : config.getRsiSellThresholds()) {
                            futures.add(executor.submit(() -> new ParamResult(
                                    rsiPeriod, emaShort, emaLong, buyTh, sellTh,
                                    evaluateStrategy(chatId, candles, rsiPeriod, emaShort, emaLong, buyTh, sellTh, config)
                            )));
                        }
                    }
                }
            }
        }

        final int candleCount = candles.size();

        return futures.stream()
                .map(f -> {
                    try {
                        return f.get(10, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(p -> p.score))
                .map(best -> {
                    RsiEmaStrategySettings cfg = new RsiEmaStrategySettings();
                    cfg.setRsiPeriod(best.rsiPeriod);
                    cfg.setEmaShort(best.emaShort);
                    cfg.setEmaLong(best.emaLong);
                    cfg.setRsiBuyThreshold(best.rsiBuy);
                    cfg.setRsiSellThreshold(best.rsiSell);
                    cfg.setTakeProfitPct(config.getTakeProfitPct());
                    cfg.setStopLossPct(config.getStopLossPct());
                    cfg.setTakeProfitWindow(config.getTakeProfitWindow());
                    cfg.setCachedCandlesLimit(candleCount);
                    return cfg;
                })
                .orElseThrow(() -> new IllegalStateException("Не удалось подобрать параметры"));
    }

    private int evaluateStrategy(Long chatId, List<Candle> candles, int rsiPeriod, int emaShort, int emaLong,
                                 double rsiBuy, double rsiSell, RsiEmaStrategySettings config) {

        int hits = 0;
        int tpWindow = config.getTakeProfitWindow();
        double tpPct = config.getTakeProfitPct();
        double slPct = config.getStopLossPct();

        List<Double> closes = candles.stream().map(Candle::getClose).collect(Collectors.toList());

        for (int i = 50; i < closes.size() - tpWindow; i++) {
            double rsi = RsiCalculator.latest(closes.subList(i - rsiPeriod, i), rsiPeriod);
            double emaS = EmaCalculator.latest(closes.subList(i - emaShort, i), emaShort);
            double emaL = EmaCalculator.latest(closes.subList(i - emaLong, i), emaLong);

            String signal;
            if (rsi < rsiBuy && emaS > emaL) signal = "BUY";
            else if (rsi > rsiSell && emaS < emaL) signal = "SELL";
            else {
                signal = "HOLD";
            }

            if (signal.equals("HOLD")) continue;

            List<Double> features = closes.subList(i - 20, i);
            if (!mlSignalFilter.isSignalApproved(chatId, features)) continue;

            double entryPrice = closes.get(i);
            List<Double> future = closes.subList(i + 1, i + 1 + tpWindow);

            boolean hit = future.stream().anyMatch(p -> {
                double pct = (p - entryPrice) / entryPrice;
                return (signal.equals("BUY") && (pct >= tpPct || pct <= -slPct)) ||
                       (signal.equals("SELL") && (pct <= -tpPct || pct >= slPct));
            });

            if (hit) hits++;
        }

        return hits;
    }

    private record ParamResult(int rsiPeriod, int emaShort, int emaLong, double rsiBuy, double rsiSell, int score) {}
}
