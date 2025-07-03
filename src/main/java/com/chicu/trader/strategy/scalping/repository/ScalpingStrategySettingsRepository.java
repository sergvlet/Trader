package com.chicu.trader.strategy.scalping.repository;

import com.chicu.trader.strategy.scalping.model.ScalpingStrategySettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScalpingStrategySettingsRepository extends JpaRepository<ScalpingStrategySettings, Long> {
}
