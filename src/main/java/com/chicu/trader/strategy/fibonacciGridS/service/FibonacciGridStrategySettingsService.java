package com.chicu.trader.strategy.fibonacciGridS.service;


import com.chicu.trader.strategy.fibonacciGridS.model.FibonacciGridStrategySettings;

public interface FibonacciGridStrategySettingsService {
    FibonacciGridStrategySettings getOrCreate(Long chatId);
    void save(FibonacciGridStrategySettings settings);
}
