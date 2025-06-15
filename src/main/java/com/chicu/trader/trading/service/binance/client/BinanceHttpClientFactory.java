package com.chicu.trader.trading.service.binance.client;

import com.chicu.trader.bot.service.ApiCredentials;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BinanceHttpClientFactory {

    public BinanceHttpClient create(String apiKey, String secretKey, boolean testnet) {
        String baseUrl = testnet
                ? "https://testnet.binance.vision"
                : "https://api.binance.com";

        return new BinanceHttpClient(apiKey, secretKey, baseUrl);
    }
}
