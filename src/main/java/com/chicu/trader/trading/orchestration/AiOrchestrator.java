package com.chicu.trader.trading.orchestration;

import com.chicu.trader.trading.optimizer.ProfitOptimizer;
import com.chicu.trader.trading.scanner.MarketScanner;
import com.chicu.trader.trading.executor.TradingExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiOrchestrator {

    private final MarketScanner marketScanner;
    private final ProfitOptimizer profitOptimizer;
    private final TradingExecutor tradingExecutor;

    /**
     * Периодическая полная переоценка стратегий и запуск торговли.
     */
    @Scheduled(cron = "0 0 * * * *") // каждый час
    public void runFullOrchestration() {
        log.info("🚀 Запуск полного цикла AI Orchestration");

        // Для всех пользователей, кто включил AI-торговлю (здесь пока только пример, можно потом расширить)
        List<Long> activeUsers = List.of(5316412277L);  // ⚠ здесь пока захардкожено, потом заменим на сервис AiTradingSettingsService

        for (Long chatId : activeUsers) {
            try {
                orchestrateForUser(chatId);
            } catch (Exception e) {
                log.error("Ошибка orchestration для chatId={}: {}", chatId, e.getMessage());
            }
        }
    }

    private void orchestrateForUser(Long chatId) {
        log.info("▶ Обработка пользователя: {}", chatId);

        // 1️⃣ Сканируем лучшие пары
        List<String> topSymbols = marketScanner.scanTopSymbols(5, Duration.ofMinutes(15));
        log.info("✅ Найдены топ монеты: {}", topSymbols);

        // 2️⃣ Оптимизируем параметры TP/SL
        profitOptimizer.optimize(chatId, topSymbols, "15m");

        // 3️⃣ Запускаем торговлю
        tradingExecutor.execute();
    }
}
