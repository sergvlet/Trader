package com.chicu.trader.trading.service.binance.client;

import com.chicu.trader.bot.entity.UserSettings;
import com.chicu.trader.bot.service.UserSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BinanceRestClientFactory {

    private final UserSettingsService userSettingsService;
    private final BinanceHttpClientFactory httpClientFactory;

    public BinanceRestClient getClient(Long chatId) {
        UserSettings userSettings = userSettingsService.getSettings(chatId);
        boolean isTestnet = "TEST".equalsIgnoreCase(userSettings.getMode());

        String apiKey = isTestnet ? userSettings.getTestApiKey() : userSettings.getRealApiKey();
        String secretKey = isTestnet ? userSettings.getTestSecretKey() : userSettings.getRealSecretKey();

        BinanceHttpClient httpClient = httpClientFactory.create(apiKey, secretKey, isTestnet);
        return new BinanceRestClient(apiKey, secretKey, isTestnet, httpClient);
    }

    public BinanceRestClient getPublicClient() {
        BinanceHttpClient httpClient = httpClientFactory.create(null, null, false);
        return new BinanceRestClient(null, null, false, httpClient);
    }
}
