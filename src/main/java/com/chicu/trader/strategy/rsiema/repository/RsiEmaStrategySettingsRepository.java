package com.chicu.trader.strategy.rsiema.repository;

import com.chicu.trader.strategy.rsiema.model.RsiEmaStrategySettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RsiEmaStrategySettingsRepository extends JpaRepository<RsiEmaStrategySettings, Long> {

    Optional<RsiEmaStrategySettings> findByAiTradingSettings_ChatId(Long chatId);
}
