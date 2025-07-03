package com.chicu.trader.strategy.rsiema.service;

import com.chicu.trader.bot.config.AiTradingDefaults;
import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.repository.AiTradingSettingsRepository;
import com.chicu.trader.strategy.rsiema.model.RsiEmaStrategySettings;
import com.chicu.trader.strategy.rsiema.repository.RsiEmaStrategySettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RsiEmaStrategySettingsService {

    private final RsiEmaStrategySettingsRepository repo;
    private final AiTradingSettingsRepository aiRepo;
    private final AiTradingDefaults defaults;

    @Transactional
    public RsiEmaStrategySettings getOrCreate(Long chatId) {
        return repo.findById(chatId).orElseGet(() -> createNew(chatId));
    }

    @Transactional
    public RsiEmaStrategySettings createNew(Long chatId) {
        AiTradingSettings aiSettings = aiRepo.findById(chatId)
                .orElseThrow(() -> new IllegalStateException("AI settings not found: " + chatId));

        RsiEmaStrategySettings cfg = new RsiEmaStrategySettings();
        cfg.setChatId(chatId);
        cfg.setAiTradingSettings(aiSettings);

        // Торговые параметры
        cfg.setEmaShort(defaults.getDefaultEmaShort());
        cfg.setEmaLong(defaults.getDefaultEmaLong());
        cfg.setRsiPeriod(defaults.getDefaultRsiPeriod());
        cfg.setRsiBuyThreshold(defaults.getDefaultRsiBuyThreshold());
        cfg.setRsiSellThreshold(defaults.getDefaultRsiSellThreshold());
        cfg.setTakeProfitPct(defaults.getDefaultTakeProfitPct());
        cfg.setStopLossPct(defaults.getDefaultStopLossPct());
        cfg.setTakeProfitWindow(defaults.getDefaultTakeProfitWindow());
        cfg.setCachedCandlesLimit(defaults.getDefaultCachedCandlesLimit());
        cfg.setTimeframe(defaults.getDefaultTimeframe());

        // ML-параметры для обучения
        cfg.setRsiPeriods(defaults.getDefaultRsiPeriods());
        cfg.setEmaShorts(defaults.getDefaultEmaShorts());
        cfg.setEmaLongs(defaults.getDefaultEmaLongs());
        cfg.setRsiBuyThresholds(defaults.getDefaultRsiBuyThresholds());
        cfg.setRsiSellThresholds(defaults.getDefaultRsiSellThresholds());

        return repo.saveAndFlush(cfg);
    }

    @Transactional
    public void save(RsiEmaStrategySettings cfg) {
        repo.saveAndFlush(cfg);
    }

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

    @Transactional
    public void resetTrainingRanges(Long chatId) {
        RsiEmaStrategySettings cfg = getOrCreate(chatId);
        cfg.setRsiPeriods(defaults.getDefaultRsiPeriods());
        cfg.setEmaShorts(defaults.getDefaultEmaShorts());
        cfg.setEmaLongs(defaults.getDefaultEmaLongs());
        cfg.setRsiBuyThresholds(defaults.getDefaultRsiBuyThresholds());
        cfg.setRsiSellThresholds(defaults.getDefaultRsiSellThresholds());
        cfg.setTakeProfitPct(defaults.getDefaultTakeProfitPct());
        cfg.setStopLossPct(defaults.getDefaultStopLossPct());
        cfg.setTakeProfitWindow(defaults.getDefaultTakeProfitWindow());
        repo.saveAndFlush(cfg);
    }
}
