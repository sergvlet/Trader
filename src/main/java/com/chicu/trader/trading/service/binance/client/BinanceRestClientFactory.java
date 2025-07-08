package com.chicu.trader.trading.service.binance.client;

import com.chicu.trader.bot.entity.UserSettings;
import com.chicu.trader.bot.service.UserSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Фабрика для создания BinanceRestClient:
 * - приватного, с ключами пользователя (для торговли),
 * - публичного, без ключей (только market-data),
 * - произвольного (по явно заданным ключам и testnet-флагу).
 */
@Component
@RequiredArgsConstructor
public class BinanceRestClientFactory {

    private final UserSettingsService      userSettingsService;
    private final BinanceHttpClientFactory httpClientFactory;

    /**
     * Клиент с ключами из настроек пользователя.
     */
    public BinanceRestClient getClient(Long chatId) {
        UserSettings settings = userSettingsService.getSettings(chatId);
        boolean isTestnet     = "TEST".equalsIgnoreCase(settings.getMode());

        String apiKey    = isTestnet
                ? settings.getTestApiKey()
                : settings.getRealApiKey();
        String secretKey = isTestnet
                ? settings.getTestSecretKey()
                : settings.getRealSecretKey();

        BinanceHttpClient httpClient = httpClientFactory.create(apiKey, secretKey, isTestnet);
        return new BinanceRestClient(apiKey, secretKey, isTestnet, httpClient);
    }

    /**
     * Публичный клиент без ключей (использует production-эндпоинты).
     */
    public BinanceRestClient getPublicClient() {
        BinanceHttpClient httpClient = httpClientFactory.create(null, null, false);
        return new BinanceRestClient(null, null, false, httpClient);
    }

    /**
     * Клиент по явным параметрам: ключи + режим.
     */
    public BinanceRestClient create(String apiKey, String secretKey, boolean isTestnet) {
        BinanceHttpClient httpClient = httpClientFactory.create(apiKey, secretKey, isTestnet);
        return new BinanceRestClient(apiKey, secretKey, isTestnet, httpClient);
    }
}
