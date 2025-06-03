package com.chicu.trader.strategy.rsiema.service;

import com.chicu.trader.bot.config.AiTradingDefaults;
import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.repository.AiTradingSettingsRepository;
import com.chicu.trader.strategy.rsiema.model.RsiEmaStrategySettings;
import com.chicu.trader.strategy.rsiema.repository.RsiEmaStrategySettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис для работы с настройками RSI/EMA-стратегии пользователя.
 */
@Service
@RequiredArgsConstructor
public class RsiEmaStrategySettingsService {

    private final RsiEmaStrategySettingsRepository repo;
    private final AiTradingSettingsRepository aiRepo;
    private final AiTradingDefaults defaults;

    /**
     * Возвращает существующие настройки или создаёт новые с дефолтными значениями.
     */
    @Transactional
    public RsiEmaStrategySettings getOrCreate(Long chatId) {
        return repo.findByAiTradingSettings_ChatId(chatId)

                .orElseGet(() -> createNew(chatId));
    }

    /**
     * Создаёт новую запись с параметрами по умолчанию из AiTradingDefaults.
     */
    @Transactional
    public RsiEmaStrategySettings createNew(Long chatId) {
        AiTradingSettings aiSettings = aiRepo.findById(chatId)
                .orElseThrow(() -> new IllegalStateException("AI settings not found: " + chatId));

        RsiEmaStrategySettings cfg = new RsiEmaStrategySettings();
        cfg.setChatId(chatId);
        cfg.setAiTradingSettings(aiSettings);
        cfg.setEmaShort(defaults.getDefaultEmaShort());
        cfg.setEmaLong(defaults.getDefaultEmaLong());
        cfg.setRsiPeriod(defaults.getDefaultRsiPeriod());
        cfg.setRsiBuyThreshold(defaults.getDefaultRsiBuyThreshold());
        cfg.setRsiSellThreshold(defaults.getDefaultRsiSellThreshold());
        cfg.setCachedCandlesLimit(defaults.getDefaultCachedCandlesLimit());

        return repo.saveAndFlush(cfg);
    }

    /**
     * Сохраняет любые изменения в настройках сразу.
     */
    @Transactional
    public void save(RsiEmaStrategySettings cfg) {
        repo.saveAndFlush(cfg);
    }

    // ----- Методы для сброса параметров к дефолтным -----

    @Transactional
    public void resetRsiDefaults(Long chatId) {
        RsiEmaStrategySettings cfg = getOrCreate(chatId);
        cfg.setRsiPeriod(defaults.getDefaultRsiPeriod());
        cfg.setRsiBuyThreshold(defaults.getDefaultRsiBuyThreshold());
        cfg.setRsiSellThreshold(defaults.getDefaultRsiSellThreshold());
        repo.saveAndFlush(cfg);
    }

    @Transactional
    public void resetEmaDefaults(Long chatId) {
        RsiEmaStrategySettings cfg = getOrCreate(chatId);
        cfg.setEmaShort(defaults.getDefaultEmaShort());
        cfg.setEmaLong(defaults.getDefaultEmaLong());
        repo.saveAndFlush(cfg);
    }
}
