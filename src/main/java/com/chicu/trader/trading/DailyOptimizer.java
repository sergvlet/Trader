// src/main/java/com/chicu/trader/trading/DailyOptimizer.java
package com.chicu.trader.trading;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.service.CandleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyOptimizer {

    private final AiTradingSettingsService aiSettingsService;
    private final CandleService candleService;

    public OptimizationResult optimizeAllForChat(Long chatId) {
        log.info("🚀 Запуск оптимизации параметров для chatId={}", chatId);

        List<Candle> candles = historyForChat(chatId);

        // Здесь твоя логика оптимизации на основе свечей:
        // Например — подбираем tp/sl, timeframe, pairs, ...
        // Пока заглушка:
        return OptimizationResult.builder()
                .tp(0.03)
                .sl(0.01)
                .symbols(List.of("BTCUSDT", "ETHUSDT"))
                .topN(2)
                .timeframe("1h")
                .riskThreshold(0.1)
                .maxDrawdown(0.2)
                .leverage(3)
                .maxPositions(2)
                .tradeCooldown(15)
                .slippageTolerance(0.01)
                .orderType("MARKET")
                .notificationsEnabled(true)
                .modelVersion("v1")
                .build();
    }

    public List<Candle> historyForChat(Long chatId) {
        AiTradingSettings settings = aiSettingsService.getOrCreate(chatId);

        String symbol = settings.getSymbols() != null
                ? settings.getSymbols().split(",")[0]
                : "BTCUSDT";

        Duration timeframe = Duration.ofHours(1); // или преобразуй settings.getTimeframe()
        int limit = 120;

        log.info("📥 Загрузка истории свечей для chatId={} symbol={} limit={}", chatId, symbol, limit);

        return candleService.history(symbol, timeframe, limit);
    }
}
