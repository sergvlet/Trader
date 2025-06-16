package com.chicu.trader.trading.service.binance.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Фабрика для создания BinanceHttpClient:
 *  - публичного (market-data-only) без ключей,
 *  - приватного с ключами пользователя.
 */
@Component
public class BinanceHttpClientFactory {

    @Value("${binance.base-url}")
    private String baseUrl;

    @Value("${binance.testnet-base-url}")
    private String testnetBaseUrl;

    /**
     * @param apiKey     API-ключ (null для публичного клиента)
     * @param secretKey  Секрет (null для публичного клиента)
     * @param isTestnet  Если true — берем testnetBaseUrl, иначе — production baseUrl
     */
    public BinanceHttpClient create(String apiKey, String secretKey, boolean isTestnet) {
        String url = isTestnet ? testnetBaseUrl : baseUrl;

        // Если ключей нет — создаем "public" клиент через конструктор без ключей
        if (apiKey == null || secretKey == null) {
            return new BinanceHttpClient(url);
        }

        // Иначе — полный клиент с ключами
        return new BinanceHttpClient(apiKey, secretKey, url);
    }
}
