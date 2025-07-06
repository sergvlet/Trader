package com.chicu.trader.trading.backtest.service;

import com.chicu.trader.trading.model.BacktestSettings;

import java.time.LocalDate;

public interface BacktestSettingsService {

    BacktestSettings getOrCreate(Long chatId);

    void save(BacktestSettings settings);

    void updatePeriod(Long chatId, LocalDate start, LocalDate end);

    void updateCommission(Long chatId, Double commissionPct);

    void updateTimeframe(Long chatId, String timeframe);

    void updateCachedCandlesLimit(Long chatId, int limit);

    void updateLeverage(Long chatId, int leverage);

    void updateSlippage(Long chatId, Double slippagePct);
}
