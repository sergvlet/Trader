package com.chicu.trader.bot.service;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.repository.AiTradingSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTradingService {

    private final AiTradingSettingsService settingsService;
    private final AiTradingSettingsRepository settingsRepo;

    /**
     * Включить торговлю (активировать для TradingExecutor).
     */
    public void enableTrading(Long chatId) {
        AiTradingSettings settings = settingsService.getOrCreate(chatId);
        settings.setIsRunning(true);
        settingsRepo.save(settings);
        log.info("✅ Торговля включена для chatId={}", chatId);
    }

    /**
     * Отключить торговлю.
     */
    public void disableTrading(Long chatId) {
        AiTradingSettings settings = settingsService.getOrCreate(chatId);
        settings.setIsRunning(false);
        settingsRepo.save(settings);
        log.info("⏹️ Торговля остановлена для chatId={}", chatId);
    }

    /**
     * Проверить, включена ли торговля.
     */
    public boolean isTradingEnabled(Long chatId) {
        AiTradingSettings settings = settingsService.getOrCreate(chatId);
        return Boolean.TRUE.equals(settings.getIsRunning());
    }
}
