package com.chicu.trader.trading.executor;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.service.AiTradingSettingsService;
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
public class TradingOrchestrator {

    private final AiTradingSettingsService settingsService;
    private final ProfitablePairService pairService;
    private final TradingStrategyProcessor strategyProcessor;

    /**
     * Основной планировщик, который проверяет всех пользователей и запускает стратегии.
     * Запускается каждые 15 секунд.
     */
    @Scheduled(fixedRate = 15_000)
    public void execute() {
        log.info("🚀 Запуск TradingOrchestrator 2.0");

        List<AiTradingSettings> allSettings = settingsService.getAllActiveTrading();
        for (AiTradingSettings settings : allSettings) {
            Long chatId = settings.getChatId();
            List<ProfitablePair> pairs = pairService.getActivePairs(chatId);

            for (ProfitablePair pair : pairs) {
                strategyProcessor.processSymbol(chatId, pair);
            }
        }
    }
}
