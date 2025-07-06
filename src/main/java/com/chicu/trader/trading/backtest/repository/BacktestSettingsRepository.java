// src/main/java/com/chicu/trader/trading/repository/BacktestSettingsRepository.java
package com.chicu.trader.trading.backtest.repository;

import com.chicu.trader.trading.model.BacktestSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BacktestSettingsRepository extends JpaRepository<BacktestSettings, Long> { }
