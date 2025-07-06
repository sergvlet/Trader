// src/main/java/com/chicu/trader/trading/service/BacktestService.java
package com.chicu.trader.trading.backtest.service;


import com.chicu.trader.trading.backtest.BacktestResult;

public interface BacktestService {
    BacktestResult runBacktest(Long chatId);
}
