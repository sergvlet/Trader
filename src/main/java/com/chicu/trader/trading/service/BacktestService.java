// src/main/java/com/chicu/trader/trading/service/BacktestService.java
package com.chicu.trader.trading.service;

import com.chicu.trader.trading.model.BacktestResult;

public interface BacktestService {
    BacktestResult runBacktest(Long chatId);
}
