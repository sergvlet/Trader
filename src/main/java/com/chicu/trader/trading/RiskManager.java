// src/main/java/com/chicu/trader/trading/RiskManager.java
package com.chicu.trader.trading;

import com.chicu.trader.bot.entity.UserSettings;
import com.chicu.trader.bot.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RiskManager {

    private final BalanceService balanceService;
    private final UserSettingsRepository settingsRepo;

    /**
     * Разрешает новые сделки, если нет серьёзной просадки или блокировки по таймеру.
     */
    public boolean allowNewTrades(Long chatId) {
        UserSettings settings = settingsRepo.findById(chatId)
            .orElseThrow(() -> new IllegalStateException("No settings for user " + chatId));

        double currentEquity = balanceService.getAvailableUsdt(chatId);
        // Если maxEquity ещё не задан — инициализируем
        Double maxEquity = settings.getMaxEquity();
        if (maxEquity == null) {
            settings.setMaxEquity(currentEquity);
            settingsRepo.save(settings);
            maxEquity = currentEquity;
        }

        // Если текущий баланс превысил прошлый максимум — обновляем maxEquity
        if (currentEquity > maxEquity) {
            settings.setMaxEquity(currentEquity);
            settingsRepo.save(settings);
            maxEquity = currentEquity;
        }

        // Проверяем, заблокирован ли пользователь по времени
        Long blockedUntil = settings.getNextAllowedTradeTime();
        if (blockedUntil != null && blockedUntil > System.currentTimeMillis()) {
            return false;
        }

        // Если просадка более 10% — блокируем на 24 часа
        if (currentEquity < maxEquity * 0.9) {
            long unblockAt = System.currentTimeMillis() + Duration.ofHours(24).toMillis();
            settings.setNextAllowedTradeTime(unblockAt);
            settingsRepo.save(settings);
            return false;
        }

        return true;
    }
}
