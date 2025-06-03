package com.chicu.trader.strategy.ml;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий для работы с настройками стратегии ML_MODEL.
 */
@Repository
public interface MlModelStrategySettingsRepository extends JpaRepository<MlModelStrategySettings, Long> {
    // chatId — это и PK, и FK
}
