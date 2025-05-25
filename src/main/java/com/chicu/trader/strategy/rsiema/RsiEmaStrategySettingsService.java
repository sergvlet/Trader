package com.chicu.trader.strategy.rsiema;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.repository.AiTradingSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RsiEmaStrategySettingsService {

    private final RsiEmaStrategySettingsRepository repo;
    private final AiTradingSettingsRepository aiRepo;

    @Transactional
    public RsiEmaStrategySettings getOrCreate(Long chatId) {
        return repo.findById(chatId).orElseGet(() -> createNew(chatId));
    }

    @Transactional
    public RsiEmaStrategySettings createNew(Long chatId) {
        AiTradingSettings s = aiRepo.findById(chatId)
                .orElseThrow(() -> new IllegalStateException("AI settings not found: " + chatId));
        RsiEmaStrategySettings defaults = RsiEmaStrategySettings.builder()
                .aiSettings(s)
                .chatId(chatId)
                .emaShort(9)
                .emaLong(21)
                .rsiPeriod(14)
                .rsiBuyThreshold(30.0)
                .rsiSellThreshold(70.0)
                .build();
        return repo.saveAndFlush(defaults);
    }

    public void save(RsiEmaStrategySettings cfg) {
        repo.save(cfg);
    }

    @Transactional
    public void resetRsiDefaults(Long chatId) {
        RsiEmaStrategySettings cfg = getOrCreate(chatId);
        cfg.setRsiPeriod(14);
        cfg.setRsiBuyThreshold(30.0);
        cfg.setRsiSellThreshold(70.0);
        save(cfg);
    }

    @Transactional
    public void resetEmaDefaults(Long chatId) {
        RsiEmaStrategySettings cfg = getOrCreate(chatId);
        cfg.setEmaShort(9);
        cfg.setEmaLong(21);
        save(cfg);
    }
}
