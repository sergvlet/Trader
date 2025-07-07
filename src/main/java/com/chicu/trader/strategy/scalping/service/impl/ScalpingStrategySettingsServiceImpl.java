package com.chicu.trader.strategy.scalping.service.impl;

import com.chicu.trader.bot.config.AiTradingDefaults;
import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.repository.AiTradingSettingsRepository;
import com.chicu.trader.strategy.scalping.model.ScalpingStrategySettings;
import com.chicu.trader.strategy.scalping.repository.ScalpingStrategySettingsRepository;
import com.chicu.trader.strategy.scalping.service.ScalpingStrategySettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScalpingStrategySettingsServiceImpl implements ScalpingStrategySettingsService {

    private final ScalpingStrategySettingsRepository repo;
    private final AiTradingSettingsRepository aiRepo;
    private final AiTradingDefaults defaults;

    @Override
    @Transactional
    public ScalpingStrategySettings getOrCreate(Long chatId) {
        return repo.findById(chatId)
                .orElseGet(() -> createDefault(chatId));
    }

    private ScalpingStrategySettings createDefault(Long chatId) {
        AiTradingSettings aiSettings = aiRepo.findById(chatId)
                .orElseThrow(() -> new IllegalStateException(
                        "AiTradingSettings not found for chatId: " + chatId));

        ScalpingStrategySettings settings = ScalpingStrategySettings.builder()
                .chatId(chatId)
                .aiTradingSettings(aiSettings)
                .windowSize(5)
                .priceChangeThreshold(0.2)
                .minVolume(10.0)
                .spreadThreshold(0.2)
                .takeProfitPct(1.0)
                .stopLossPct(0.5)
                .timeframe(defaults.getDefaultTimeframe())
                .cachedCandlesLimit(defaults.getDefaultCachedCandlesLimit())// Новые поля
                .volumeThresholdMultiplier(1.0)  // тут можно взять из defaults, если добавите
                .emaSmoothing(2)                  // тоже можно вынести в defaults
                .symbol(
                        aiSettings.getSymbols() != null
                                ? aiSettings.getSymbols()
                                : ""
                )
                .build();

        return repo.saveAndFlush(settings);
    }

    @Override
    @Transactional
    public void save(ScalpingStrategySettings settings) {
        repo.saveAndFlush(settings);
    }
}
