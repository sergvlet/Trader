// src/main/java/com/chicu/trader/trading/DailyOptimizer.java
package com.chicu.trader.trading;

import com.chicu.trader.bot.config.AiTradingDefaults;
import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.optimizer.TpSlOptimizer;
import com.chicu.trader.trading.service.CandleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyOptimizer {

    private final CandleService candleService;
    private final TpSlOptimizer             optimizer;
    private final AiTradingSettingsService  settingsService;
    private final AiTradingDefaults         defaults;

    /**
     * Ежедневная оптимизация всех параметров.
     * Запускается в 03:00 Europe/Warsaw.
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Warsaw")
    public void nightlyUpdate() {
        log.info("Запускаю ночную оптимизацию для всех пользователей");
        settingsService.findAllChatIds()
                .forEach(this::optimizeAllForChat);
    }

    /**
     * Подобрать оптимальные значения TP/SL, TopN, список символов,
     * таймфрейм, риск и макс. просадку для конкретного chatId.
     */
    public OptimizationResult optimizeAllForChat(Long chatId) {
        AiTradingSettings settings = settingsService.getOrCreate(chatId);

        // 1) Собираем список символов
        List<String> symbols = settings.getSymbols() != null && !settings.getSymbols().isBlank()
                ? List.of(settings.getSymbols().split(","))
                : List.of();

        // 2) Составляем объединённый список свечей
        List<Candle> allCandles = symbols.stream()
                .flatMap(sym -> candleService.historyHourly(chatId, sym, 100).stream())
                .collect(Collectors.toList());

        // 3) TP/SL оптимизация по списку свечей
        TpSlOptimizer.Result tpSl = optimizer.optimize(allCandles);

        // 4) Top N
        int topN = settings.getTopN();

        // 5) Таймфрейм, риск и макс. просадка
        String timeframe = settings.getTimeframe();
        double riskThreshold = settings.getRiskThreshold() != null
                ? settings.getRiskThreshold()
                : defaults.getDefaultRiskThreshold();
        double maxDrawdown = settings.getMaxDrawdown() != null
                ? settings.getMaxDrawdown()
                : defaults.getDefaultMaxDrawdown();

        log.info("Оптимизация для chatId={} завершена: TP={} SL={}",
                chatId, tpSl.getTpPct(), tpSl.getSlPct());

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
}
