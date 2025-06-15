package com.chicu.trader.trading.executor;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.trading.TradeOrchestrator;
import com.chicu.trader.trading.entity.ProfitablePair;
import com.chicu.trader.trading.service.ProfitablePairService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradingExecutor {

    private final AiTradingSettingsService settingsService;
    private final ProfitablePairService profitablePairService;
    private final TradeOrchestrator tradeOrchestrator;

    // Основной плановый запуск каждые 60 сек
    @Scheduled(fixedRate = 60_000)
    public void execute() {
        List<AiTradingSettings> activeUsers = settingsService.findAllRunning();

        for (AiTradingSettings settings : activeUsers) {
            Long chatId = settings.getChatId();
            executeSingle(chatId);
        }
    }

    // Новый метод — запуск по одному пользователю (используется в Telegram меню)
    public void startSingle(Long chatId) {
        log.info("▶ Старт торговли для chatId={}", chatId);
        executeSingle(chatId);
    }

    // Заглушка под остановку (можно будет доработать)
    public void stopSingle(Long chatId) {
        log.info("⏹ Стоп торговли для chatId={}", chatId);
    }

    // Универсальный метод обработки одного пользователя
    private void executeSingle(Long chatId) {
        List<ProfitablePair> pairs = profitablePairService.getActivePairs(chatId);
        if (pairs.isEmpty()) {
            log.info("Нет активных пар для chatId={}", chatId);
            return;
        }
        try {
            tradeOrchestrator.apply(chatId, pairs);
        } catch (Exception e) {
            log.error("Ошибка при обработке торговли для chatId={}: {}", chatId, e.getMessage(), e);
        }
    }
}
