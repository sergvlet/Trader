// src/main/java/com/chicu/trader/trading/service/binance/BinanceExchangeInfoService.java
package com.chicu.trader.trading.service.binance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Загружает /api/v3/exchangeInfo один раз при старте
 * и кеширует tickSize и stepSize для всех символов.
 */
@Slf4j
@Service
public class BinanceExchangeInfoService {

    private static final String EXCHANGE_INFO_URL = "https://api.binance.com/api/v3/exchangeInfo";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Шаги лота (LOT_SIZE.stepSize) для каждого символа */
    @Getter
    private final Map<String, BigDecimal> lotSizeSteps = new ConcurrentHashMap<>();

    /** Шаги цены (PRICE_FILTER.tickSize) для каждого символа */
    @Getter
    private final Map<String, BigDecimal> priceTickSizes = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("Loading Binance exchangeInfo from {}", EXCHANGE_INFO_URL);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EXCHANGE_INFO_URL))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new RuntimeException("Failed to load exchangeInfo, HTTP status: " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode symbols = root.path("symbols");
            if (!symbols.isArray()) {
                throw new RuntimeException("Unexpected exchangeInfo format: 'symbols' is not an array");
            }

            for (JsonNode symbolNode : symbols) {
                String symbol = symbolNode.path("symbol").asText();
                JsonNode filters = symbolNode.path("filters");
                if (!filters.isArray()) continue;

                for (JsonNode filter : filters) {
                    String type = filter.path("filterType").asText();
                    switch (type) {
                        case "LOT_SIZE":
                            String stepSize = filter.path("stepSize").asText();
                            lotSizeSteps.put(symbol, new BigDecimal(stepSize));
                            break;
                        case "PRICE_FILTER":
                            String tickSize = filter.path("tickSize").asText();
                            priceTickSizes.put(symbol, new BigDecimal(tickSize));
                            break;
                        default:
                            // остальные фильтры игнорируем
                    }
                }
            }

            log.info("Loaded exchangeInfo: {} symbols, lotSizeSteps={}, priceTickSizes={}",
                    lotSizeSteps.size(), lotSizeSteps.size(), priceTickSizes.size());

        } catch (Exception e) {
            log.error("Failed to load or parse Binance exchangeInfo", e);
            throw new RuntimeException("Could not initialize BinanceExchangeInfoService", e);
        }
    }

    /**
     * Возвращает шаг лота (stepSize) для указанного символа.
     * Если символ неизвестен — возвращает 1.
     */
    public BigDecimal getLotSizeStep(String symbol) {
        return lotSizeSteps.getOrDefault(symbol, BigDecimal.ONE);
    }

    /**
     * Возвращает шаг цены (tickSize) для указанного символа.
     * Если символ неизвестен — возвращает 1.
     */
    public BigDecimal getPriceTickSize(String symbol) {
        return priceTickSizes.getOrDefault(symbol, BigDecimal.ONE);
    }
}
