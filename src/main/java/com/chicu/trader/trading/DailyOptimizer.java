package com.chicu.trader.trading;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.service.CandleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DailyOptimizer — сервис для автоматической оптимизации торговых параметров в AI-режиме.
 * <p>
 * 🔹 Основные задачи:
 * — Загружает исторические свечи по списку валютных пар.
 * — Анализирует волатильность и тренд по каждой паре.
 * — Выбирает наиболее перспективные пары (по соотношению тренда к амплитуде).
 * — Рассчитывает рекомендованные TP и SL на основе поведения цены.
 * — Возвращает полностью заполненный OptimizationResult, готовый к применению.
 * <p>
 * ⚙️ Параметры берутся из AiTradingSettings (например, список пар, таймфрейм, topN и пр.)
 * 📊 Все расчёты выполняются по реальным данным с биржи через CandleService.
 * <p>
 * TODO:
 * - [ ] Добавить расчёт ATR / Bollinger Bands / RSI как дополнительные признаки.
 * - [ ] Учитывать корреляции между парами.
 * - [ ] Поддержка взвешенного выбора пар (например, по средней доходности).
 * - [ ] Настройка стратегии на основе режима торговли (например, SPOT / FUTURES).
 * - [ ] Визуализация оптимизации в Telegram.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyOptimizer {

    private final AiTradingSettingsService aiSettingsService;
    private final CandleService candleService;

    public OptimizationResult optimizeAllForChat(Long chatId) {
        log.info("🚀 Оптимизация параметров для chatId={}", chatId);

        AiTradingSettings settings = aiSettingsService.getOrCreate(chatId);

        List<String> symbols = getSymbols(settings);
        Duration timeframe = parseTimeframe(settings.getTimeframe());
        int candleLimit = Optional.ofNullable(settings.getCachedCandlesLimit()).orElse(500);

        Map<String, Double> profitMap = new HashMap<>();
        Map<String, List<Candle>> allCandles = new HashMap<>();

        for (String symbol : symbols) {
            List<Candle> candles = candleService.history(symbol, timeframe, candleLimit);
            if (candles.size() < 50) {
                log.warn("⛔ Недостаточно данных для symbol={} ({} свечей)", symbol, candles.size());
                continue;
            }

            double score = estimateProfitability(candles);
            profitMap.put(symbol, score);
            allCandles.put(symbol, candles);
        }

        List<String> selected = profitMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(settings.getTopN())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<Candle> merged = selected.stream()
                .flatMap(s -> allCandles.get(s).stream())
                .collect(Collectors.toList());

        double recommendedTp = computeDynamicTp(merged);
        double recommendedSl = computeDynamicSl(merged);

        log.info("📊 Выбраны пары: {}, TP=%.4f, SL=%.4f", selected, recommendedTp, recommendedSl);

        return OptimizationResult.builder()
                .tp(recommendedTp)
                .sl(recommendedSl)
                .symbols(selected)
                .topN(settings.getTopN())
                .timeframe(settings.getTimeframe())
                .riskThreshold(settings.getRiskThreshold())
                .maxDrawdown(settings.getMaxDrawdown())
                .leverage(settings.getLeverage())
                .maxPositions(settings.getMaxPositions())
                .tradeCooldown(settings.getTradeCooldown())
                .slippageTolerance(settings.getSlippageTolerance())
                .orderType(settings.getOrderType())
                .notificationsEnabled(settings.getNotificationsEnabled())
                .modelVersion(settings.getModelVersion())
                .build();
    }

    private List<String> getSymbols(AiTradingSettings settings) {
        String symbolsStr = Optional.ofNullable(settings.getSymbols()).orElse("BTCUSDT,ETHUSDT");
        return Arrays.stream(symbolsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    private Duration parseTimeframe(String tf) {
        if (tf == null) return Duration.ofHours(1);
        return switch (tf.toLowerCase()) {
            case "1m" -> Duration.ofMinutes(1);
            case "5m" -> Duration.ofMinutes(5);
            case "15m" -> Duration.ofMinutes(15);
            case "1h" -> Duration.ofHours(1);
            case "4h" -> Duration.ofHours(4);
            case "1d" -> Duration.ofDays(1);
            default -> Duration.ofHours(1);
        };
    }

    private double estimateProfitability(List<Candle> candles) {
        double open = candles.get(0).getOpen();
        double close = candles.get(candles.size() - 1).getClose();
        double change = (close - open) / open;

        double sumRange = candles.stream()
                .mapToDouble(c -> c.getHigh() - c.getLow())
                .sum();
        double avgRange = sumRange / candles.size();

        if (avgRange == 0) return 0.0;
        return change / avgRange;
    }

    private double computeDynamicTp(List<Candle> candles) {
        List<Double> positives = candles.stream()
                .map(c -> c.getClose() - c.getOpen())
                .filter(v -> v > 0)
                .map(v -> v / candles.get(0).getOpen())
                .toList();
        return positives.stream().mapToDouble(d -> d).average().orElse(0.03);
    }

    private double computeDynamicSl(List<Candle> candles) {
        List<Double> negatives = candles.stream()
                .map(c -> c.getOpen() - c.getClose())
                .filter(v -> v > 0)
                .map(v -> v / candles.get(0).getOpen())
                .toList();
        return negatives.stream().mapToDouble(d -> d).average().orElse(0.01);
    }
}
