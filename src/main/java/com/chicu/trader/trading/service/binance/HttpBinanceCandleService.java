// src/main/java/com/chicu/trader/trading/service/binance/HttpBinanceCandleService.java
package com.chicu.trader.trading.service.binance;

import com.chicu.trader.bot.config.AiTradingDefaults;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.service.CandleService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class HttpBinanceCandleService implements CandleService {

    private static final String PROD_REST = "https://api.binance.com";
    private static final String TEST_REST = "https://testnet.binance.vision";

    private final AiTradingDefaults defaults;
    private final HttpClient        client       = HttpClient.newHttpClient();
    private final ObjectMapper      objectMapper = new ObjectMapper();

    /**
     * Преобразует Duration в бинансовый интервал: 1m, 5m, 15m, 1h, 4h, 1d и т.д.
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
     * Получить исторические свечи через HTTP REST API Binance.
     */
    @Override
    public List<Candle> history(String symbol, Duration interval, int limit) {
        String base = defaults.getNetworkMode().equalsIgnoreCase("test") ? TEST_REST : PROD_REST;
        String binInt = toBinanceInterval(interval);
        String url = String.format("%s/api/v3/klines?symbol=%s&interval=%s&limit=%d",
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
                        node.get(0).asLong(),
                        node.get(1).asDouble(),
                        node.get(2).asDouble(),
                        node.get(3).asDouble(),
                        node.get(4).asDouble(),
                        node.get(5).asDouble(),
                        node.get(6).asLong()
                ));
            }
            return result;
        } catch (Exception e) {
            log.error("Ошибка загрузки свечей {} {}: {}", symbol, interval, e.getMessage());
            return List.of();
        }
    }

    /**
     * Обработчик входящей WS-свечи. Вызывается из WebSocket-сервиса.
     */
    @Override
    public void onWebSocketCandleUpdate(Candle candle) {
        // Здесь можно сохранять в БД, пушить в очередь или сразу обрабатывать
        log.debug("WS-свеча {}: closeTime={}, close={}",
                candle.getSymbol(), candle.getCloseTime(), candle.getClose());
        // TODO: при необходимости — передать дальше (например, в StrategyFacade)
    }
}
