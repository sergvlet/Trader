// src/main/java/com/chicu/trader/config/WebClientConfig.java
package com.chicu.trader.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(codecs -> codecs
                .defaultCodecs()
                .maxInMemorySize(16 * 1024 * 1024) // 16 MiB
            )
            .build();

        return WebClient.builder()
            .exchangeStrategies(strategies);
    }
}
