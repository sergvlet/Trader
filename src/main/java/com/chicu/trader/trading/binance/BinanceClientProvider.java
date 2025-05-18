// src/main/java/com/chicu/trader/trading/binance/BinanceClientProvider.java
package com.chicu.trader.trading.binance;

import com.chicu.trader.bot.entity.UserSettings;
import com.chicu.trader.bot.repository.UserSettingsRepository;
import com.binance.connector.client.impl.SpotClientImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BinanceClientProvider {

    private final UserSettingsRepository settingsRepo;

    public SpotClientImpl restClient(Long chatId) {
        UserSettings s = settingsRepo.findById(chatId)
            .orElseThrow(() -> new IllegalStateException("No UserSettings for chatId=" + chatId));

        String mode = s.getMode();
        String apiKey = "REAL".equalsIgnoreCase(mode) ? s.getRealApiKey() : s.getTestApiKey();
        String secret = "REAL".equalsIgnoreCase(mode) ? s.getRealSecretKey() : s.getTestSecretKey();

        if (apiKey == null || secret == null) {
            throw new IllegalStateException("API keys not set for user " + chatId + " in mode " + mode);
        }
        return new SpotClientImpl(apiKey, secret);
    }
}
