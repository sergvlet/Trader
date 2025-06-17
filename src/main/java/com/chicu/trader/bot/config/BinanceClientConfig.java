// src/main/java/com/chicu/trader/config/BinanceClientConfig.java
package com.chicu.trader.bot.config;

import com.chicu.trader.trading.service.binance.client.BinanceHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BinanceClientConfig {

    // можно брать из application.yml/properties
    @Value("${binance.api.base-url:https://api.binance.com}")
    private String baseUrl;

    @Bean
    public BinanceHttpClient binanceHttpClient() {
        // вызываем существующий конструктор с baseUrl
        return new BinanceHttpClient(baseUrl);
    }
}
