// src/main/java/com/chicu/trader/strategy/rsiema/RsiEmaStrategySettingsService.java
package com.chicu.trader.strategy.rsiema;

import com.chicu.trader.bot.config.AiTradingDefaults;
import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.repository.AiTradingSettingsRepository;
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
        return repo.findById(chatId)
                   .orElseGet(() -> createNew(chatId));
    }

    /**
     * Создаёт новую запись с параметрами по умолчанию из AiTradingDefaults.
     */
    @Transactional
    public RsiEmaStrategySettings createNew(Long chatId) {
        AiTradingSettings aiSettings = aiRepo.findById(chatId)
            .orElseThrow(() -> new IllegalStateException("AI settings not found: " + chatId));

        RsiEmaStrategySettings cfg = RsiEmaStrategySettings.builder()
            .chatId(chatId)
            .aiSettings(aiSettings)
            .emaShort(defaults.getDefaultEmaShort())
            .emaLong(defaults.getDefaultEmaLong())
            .rsiPeriod(defaults.getDefaultRsiPeriod())
            .rsiBuyThreshold(defaults.getDefaultRsiBuyThreshold())
            .rsiSellThreshold(defaults.getDefaultRsiSellThreshold())
            .build();

        return repo.saveAndFlush(cfg);
    }

    /**
     * Сохраняет любые изменения в настройках сразу и сбрасывает кэш версий.
     */
    @Transactional
    public void save(RsiEmaStrategySettings cfg) {
        repo.saveAndFlush(cfg);
    }

    // ----- Методы для обновления отдельных настроек -----

    @Transactional
    public void updateEmaShort(Long chatId, int emaShort) {
        RsiEmaStrategySettings cfg = getOrCreate(chatId);
        cfg.setEmaShort(emaShort);
        repo.saveAndFlush(cfg);
    }

    @Transactional
    public void updateEmaLong(Long chatId, int emaLong) {
        RsiEmaStrategySettings cfg = getOrCreate(chatId);
        cfg.setEmaLong(emaLong);
        repo.saveAndFlush(cfg);
    }

    @Transactional
    public void updateRsiPeriod(Long chatId, int rsiPeriod) {
        RsiEmaStrategySettings cfg = getOrCreate(chatId);
        cfg.setRsiPeriod(rsiPeriod);
        repo.saveAndFlush(cfg);
    }

    @Transactional
    public void updateRsiBuyThreshold(Long chatId, double threshold) {
        RsiEmaStrategySettings cfg = getOrCreate(chatId);
        cfg.setRsiBuyThreshold(threshold);
        repo.saveAndFlush(cfg);
    }

    @Transactional
    public void updateRsiSellThreshold(Long chatId, double threshold) {
        RsiEmaStrategySettings cfg = getOrCreate(chatId);
        cfg.setRsiSellThreshold(threshold);
        repo.saveAndFlush(cfg);
    }

    // ----- Методы для сброса к дефолтам -----

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
