package com.chicu.trader.strategy.scalping.service;

import com.chicu.trader.strategy.scalping.model.ScalpingStrategySettings;

public interface ScalpingStrategySettingsService {
    ScalpingStrategySettings getOrCreate(Long chatId);
    void save(ScalpingStrategySettings cfg);
}
