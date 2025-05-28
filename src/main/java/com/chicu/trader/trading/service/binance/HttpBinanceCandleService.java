// src/main/java/com/chicu/trader/trading/service/binance/HttpBinanceCandleService.java
package com.chicu.trader.trading.service.binance;

import com.chicu.trader.bot.config.AiTradingDefaults;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.service.CandleService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class HttpBinanceCandleService implements CandleService {

    private static final String PROD_REST = "https://api.binance.com";
    private static final String TEST_REST = "https://testnet.binance.vision";

    private final AiTradingDefaults defaults;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Кэш: строковый ключ → список свечей. Живёт 1 секунду, максимум 1000 записей.
    private final Cache<String, List<Candle>> historyCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.SECONDS)
            .maximumSize(1000)
            .build();

    /**
     * Преобразует Duration в Binance-interval: 1m, 5m, 15m, 1h, 4h, 1d и т.д.
     */
    private String toBinanceInterval(Duration interval) {
        long secs = interval.getSeconds();
        if (secs % 86400 == 0) {
            return (secs / 86400) + "d";
        } else if (secs % 3600 == 0) {
            return (secs / 3600) + "h";
        } else if (secs % 60 == 0) {
            return (secs / 60) + "m";
        }
        throw new IllegalArgumentException("Unsupported interval: " + interval);
    }

    /**
     * Получить исторические свечи через REST API Binance, с кэшированием.
     */
    @Override
    public List<Candle> history(String symbol, Duration interval, int limit) {
        String key = symbol.toUpperCase() + "|" + interval.toMillis() + "|" + limit;
        List<Candle> cached = historyCache.getIfPresent(key);
        if (cached != null) {
            log.debug("Cache HIT for history({},{},{}).", symbol, interval, limit);
            return cached;
        }

        List<Candle> fetched = fetchHistory(symbol, interval, limit);
        historyCache.put(key, fetched);
        return fetched;
    }

    /**
     * Реальное получение данных из Binance.
     */
    private List<Candle> fetchHistory(String symbol, Duration interval, int limit) {
        String base   = defaults.getNetworkMode().equalsIgnoreCase("test") ? TEST_REST : PROD_REST;
        String binInt = toBinanceInterval(interval);
        String url    = String.format("%s/api/v3/klines?symbol=%s&interval=%s&limit=%d",
                base, symbol.toUpperCase(), binInt, limit);

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode array = objectMapper.readTree(resp.body());

            List<Candle> result = new ArrayList<>(array.size());
            for (JsonNode node : array) {
                result.add(new Candle(
                        symbol.toUpperCase(),
                        node.get(0).asLong(),   // openTime
                        node.get(1).asDouble(), // open
                        node.get(2).asDouble(), // high
                        node.get(3).asDouble(), // low
                        node.get(4).asDouble(), // close
                        node.get(5).asDouble(), // volume
                        node.get(6).asLong()    // closeTime
                ));
            }
            return result;
        } catch (Exception e) {
            log.error("Ошибка загрузки свечей {} {}: {}", symbol, interval, e.getMessage());
            return List.of();
        }
    }

    /**
     * Обработчик входящей WS-свечи. Не трогаем.
     */
    @Override
    public void onWebSocketCandleUpdate(Candle candle) {
        log.debug("WS-candle {} close={} time={}",
                candle.getSymbol(), candle.getClose(), candle.getCloseTime());
    }
}
