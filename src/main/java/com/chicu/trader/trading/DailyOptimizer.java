// src/main/java/com/chicu/trader/trading/DailyOptimizer.java
package com.chicu.trader.trading;

import com.chicu.trader.bot.config.AiTradingDefaults;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.optimizer.TpSlOptimizer;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DailyOptimizer {

    private final TpSlOptimizer            optimizer;
    private final AiTradingSettingsService settingsService;
    private final AiTradingDefaults        defaults;

    /**
     * Ежедневная оптимизация всех параметров.
     * Запускается в 03:00 Europe/Warsaw.
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Warsaw")
    public void nightlyUpdate() {
        settingsService.findAllChatIds()
                .forEach(this::optimizeAllForChat);
    }

    /**
     * Подобрать оптимальные значения TP/SL, TopN, список символов,
     * таймфрейм, риск и макс. просадку для конкретного chatId.
     */
    public OptimizationResult optimizeAllForChat(Long chatId) {
        // 1) TP/SL оптимизация по истории, реализация внутри optimizer
        TpSlOptimizer.Result tpSl = optimizer.optimize(historyForChat(chatId));

        // 2) Текущие настройки пользователя
        var settings = settingsService.getOrCreate(chatId);
        int topN = settings.getTopN();
        List<String> symbols = settings.getSymbols() != null && !settings.getSymbols().isBlank()
                ? List.of(settings.getSymbols().split(","))
                : List.of();

        // 3) Таймфрейм, риск и просадка из настроек или дефолтов
        String timeframe = settings.getTimeframe();
        double riskThreshold = settings.getRiskThreshold() != null
                ? settings.getRiskThreshold()
                : defaults.getDefaultRiskThreshold();
        double maxDrawdown = settings.getMaxDrawdown() != null
                ? settings.getMaxDrawdown()
                : defaults.getDefaultMaxDrawdown();

        // 4) Собираем и возвращаем результат
        return OptimizationResult.builder()
                .tp(tpSl.getTpPct())
                .sl(tpSl.getSlPct())
                .topN(topN)
                .symbols(symbols)
                .timeframe(timeframe)
                .riskThreshold(riskThreshold)
                .maxDrawdown(maxDrawdown)
                .build();
    }

    /**
     * Заглушка: получить из базы/сервиса историю свечей,
     * необходимую optimizer'у. Реализуйте по своему усмотрению.
     */
    private List<Candle> historyForChat(Long chatId) {
        // например, settingsService или отдельный CandleService можно вызвать тут
        throw new UnsupportedOperationException("Реализуйте historyForChat(...)");
    }
}
