// src/main/java/com/chicu/trader/bot/repository/AiTradingSettingsRepository.java
package com.chicu.trader.bot.repository;

import com.chicu.trader.bot.entity.AiTradingSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiTradingSettingsRepository extends JpaRepository<AiTradingSettings, Long> {
    // по chatId
     Optional<AiTradingSettings> findByUserChatId(Long chatId);

    List<AiTradingSettings> findByIsRunningTrue();

}
