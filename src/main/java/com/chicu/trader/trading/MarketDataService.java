// src/main/java/com/chicu/trader/trading/MarketDataService.java
package com.chicu.trader.trading;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MarketDataService {

    private final WebClient webClient;

    public MarketDataService(WebClient.Builder builder) {
        // Builder уже настроен в WebClientConfig с увеличенным maxInMemorySize
        this.webClient = builder
            .baseUrl("https://api.binance.com")
            .build();
    }

    /**
     * Возвращает топ-N USDT-пар по 24-часовому объёму (quoteVolume).
     */
    @SuppressWarnings("unchecked")
    public List<String> getTopNLiquidPairs(int n) {
        // 1) Запросим весь JSON как List<Object>
        List<Object> raw = webClient.get()
            .uri("/api/v3/ticker/24hr")
            .retrieve()
            .bodyToMono(List.class)
            .block();

        // 2) Отфильтруем и приведём каждый элемент к Map<String,Object>
        List<Map<String, Object>> stats24 = raw.stream()
            .filter(o -> o instanceof Map)
            .map(o -> (Map<String, Object>) o)
            .collect(Collectors.toList());

        // 3) Отберём USDT-пары, отсортируем по quoteVolume и вернём символы
        return stats24.stream()
            .filter(entry -> {
                Object sym = entry.get("symbol");
                return sym instanceof String && ((String) sym).endsWith("USDT");
            })
            .sorted(Comparator.<Map<String, Object>>comparingDouble(entry -> {
                Object vol = entry.get("quoteVolume");
                String volStr = vol != null ? vol.toString() : "0";
                return Double.parseDouble(volStr);
            }).reversed())
            .limit(n)
            .map(entry -> (String) entry.get("symbol"))
            .collect(Collectors.toList());
    }
}
