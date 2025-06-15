package com.chicu.trader.strategy.rsiema;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.ml.MlSignalFilterService;
import com.chicu.trader.ml.MlTrainingService;
import com.chicu.trader.strategy.retrain.StrategyRetrainer;
import com.chicu.trader.strategy.rsiema.model.RsiEmaRetrainConfig;
import com.chicu.trader.strategy.rsiema.model.RsiEmaStrategySettings;
import com.chicu.trader.strategy.rsiema.service.RsiEmaRetrainConfigService;
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
    private final RsiEmaRetrainConfigService configService;
    private final MlSignalFilterService mlSignalFilter;
    private final MlTrainingService mlTrainingService;

    private final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );

    @Override
    public boolean retrain(AiTradingSettings settings) {
        Long chatId = settings.getChatId();

        // 🧠 Запуск обучения модели
        boolean trained = mlTrainingService.runTraining();
        if (!trained) {
            log.warn("⚠️ Обучение модели завершилось с ошибкой, продолжим с текущими параметрами.");
        }

        RsiEmaRetrainConfig config = configService.getOrCreateDefault(chatId);
        List<String> symbols = Arrays.stream(settings.getSymbols().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        log.info("▶️ Запуск переобучения RSI+EMA для chatId={} symbols={}", chatId, symbols);

        boolean updated = false;

        for (String symbol : symbols) {
            try {
                int candleCount = config.getTakeProfitWindow() + 100;
                List<Candle> candles = candleService.loadHistory(symbol, Duration.ofHours(1), candleCount);
                if (candles.size() < 100) continue;

                RsiEmaStrategySettings best = findBestParams(chatId, candles, config);
                best.setSymbol(symbol);
                best.setTimeframe("1h");
                best.setCachedCandlesLimit(candleCount);
                settingsService.save(best);

                log.info("✅ Переобучение завершено для chatId={} symbol={} лучшая конфигурация: {}", chatId, symbol, best);
                updated = true;
            } catch (Exception e) {
                log.error("❌ Ошибка переобучения для chatId={} symbol={}", chatId, symbol, e);
            }
        }

        return updated;
    }

    private RsiEmaStrategySettings findBestParams(Long chatId, List<Candle> candles, RsiEmaRetrainConfig config) {
        List<Future<ParamResult>> futures = new ArrayList<>();

        for (int rsiPeriod : config.getRsiPeriods()) {
            for (int emaShort : config.getEmaShorts()) {
                for (int emaLong : config.getEmaLongs()) {
                    for (double buyTh : config.getRsiBuyThresholds()) {
                        for (double sellTh : config.getRsiSellThresholds()) {
                            futures.add(executor.submit(() ->
                                    new ParamResult(
                                            rsiPeriod,
                                            emaShort,
                                            emaLong,
                                            buyTh,
                                            sellTh,
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
                    cfg.setChatId(chatId);
                    cfg.setEmaShort(best.emaShort);
                    cfg.setEmaLong(best.emaLong);
                    cfg.setRsiPeriod(best.rsiPeriod);
                    cfg.setRsiBuyThreshold(best.rsiBuy);
                    cfg.setRsiSellThreshold(best.rsiSell);
                    cfg.setCachedCandlesLimit(candleCount);
                    return cfg;
                })
                .orElseThrow(() -> new IllegalStateException("Не удалось подобрать параметры"));
    }

    private int evaluateStrategy(Long chatId,
                                 List<Candle> candles,
                                 int rsiPeriod,
                                 int emaShort,
                                 int emaLong,
                                 double rsiBuy,
                                 double rsiSell,
                                 RsiEmaRetrainConfig config) {
        int hits = 0;
        int tpWindow = config.getTakeProfitWindow();
        double tpPct = config.getTakeProfitPct();
        double slPct = config.getStopLossPct();

        List<Double> closes = candles.stream().map(Candle::getClose).collect(Collectors.toList());

        for (int i = 50; i < closes.size() - tpWindow; i++) {
            double rsi = RsiCalculator.latest(closes.subList(i - rsiPeriod, i), rsiPeriod);
            double emaS = EmaCalculator.latest(closes.subList(i - emaShort, i), emaShort);
            double emaL = EmaCalculator.latest(closes.subList(i - emaLong, i), emaLong);

            String signal = "HOLD";
            if (rsi < rsiBuy && emaS > emaL) signal = "BUY";
            else if (rsi > rsiSell && emaS < emaL) signal = "SELL";

            if (signal.equals("HOLD")) continue;

            List<Double> features = closes.subList(i - 20, i);
            boolean filtered = mlSignalFilter.isSignalApproved(chatId, features);
            if (!filtered) continue;

            double entryPrice = closes.get(i);
            List<Double> future = closes.subList(i + 1, i + 1 + tpWindow);

            final String finalSignal = signal;
            boolean hit = future.stream().anyMatch(p -> {
                double pct = (p - entryPrice) / entryPrice;
                if (finalSignal.equals("BUY")) {
                    return pct >= tpPct || pct <= -slPct;
                } else {
                    return pct <= -tpPct || pct >= slPct;
                }
            });

            if (hit) hits++;
        }

        return hits;
    }

    private record ParamResult(int rsiPeriod, int emaShort, int emaLong, double rsiBuy, double rsiSell, int score) {}
}
