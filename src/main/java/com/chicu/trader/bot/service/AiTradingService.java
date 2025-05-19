// src/main/java/com/chicu/trader/bot/service/AiTradingService.java
package com.chicu.trader.bot.service;

import com.chicu.trader.bot.entity.UserSettings;
import com.chicu.trader.bot.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTradingService {
    // Кэш состояния AI-торговли для каждого chatId (дополнительно синхронизируется с БД)
    private final Map<Long, Boolean> enabledMap = new ConcurrentHashMap<>();
    private final UserSettingsRepository userSettingsRepository;

    /** Возвращает текущее состояние AI-торговли (из кэша, по умолчанию false) */
    public boolean isTradingEnabled(Long chatId) {
        return enabledMap.getOrDefault(chatId, false);
    }

    /** Включает AI-торговлю: меняет флаг в БД и обновляет кэш */
    public void enableTrading(Long chatId) {
        setTradingFlag(chatId, true);
        log.info("✅ AI-торговля включена для chatId={}", chatId);
    }

    /** Отключает AI-торговлю: меняет флаг в БД и обновляет кэш */
    public void disableTrading(Long chatId) {
        setTradingFlag(chatId, false);
        log.info("⛔ AI-торговля отключена для chatId={}", chatId);
    }

    private void setTradingFlag(Long chatId, boolean enabled) {
        UserSettings settings = userSettingsRepository.findById(chatId)
                .orElseThrow(() -> new IllegalStateException("UserSettings not found for chatId=" + chatId));
        settings.setAiTradingEnabled(enabled);
        userSettingsRepository.save(settings);
        enabledMap.put(chatId, enabled);
    }

    /**
     * Возвращает текст последнего события AI-торговли для показа в меню.
     * Пока stub — возвращает пустую строку.
     * Можно расширить: брать из торговых логов или вести отдельный журнал.
     */
    public String getLastEvent(Long chatId) {
        return "";
    }
}
