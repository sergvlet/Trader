// src/main/java/com/chicu/trader/trading/service/binance/HttpBinanceOrderService.java
package com.chicu.trader.trading.service.binance;

import com.chicu.trader.bot.entity.UserSettings;
import com.chicu.trader.bot.service.UserSettingsService;
import com.chicu.trader.trading.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HttpBinanceOrderService implements OrderService {

    private static final String PROD_REST = "https://api.binance.com";
    private static final String TEST_REST = "https://testnet.binance.vision";

    private final UserSettingsService userSettingsService;
    private final HttpClient          httpClient = HttpClient.newHttpClient();

    private String base(Long chatId) {
        boolean isTest = userSettingsService.isTestnet(chatId);
        log.debug("HttpBinanceOrderService: base URL for chatId={} is {}", chatId, isTest ? TEST_REST : PROD_REST);
        return isTest ? TEST_REST : PROD_REST;
    }

    private String apiKey(Long chatId) {
        String key = userSettingsService.getApiCredentials(chatId).getApiKey();
        log.debug("HttpBinanceOrderService: retrieved API key for chatId={}", chatId);
        return key;
    }

    private String secretKey(Long chatId) {
        // Не логируем секретный ключ в чистом виде
        log.debug("HttpBinanceOrderService: retrieved secret key for chatId={}", chatId);
        return userSettingsService.getApiCredentials(chatId).getSecretKey();
    }

    @Override
    public void placeMarketOrder(Long chatId, String symbol, double quantity) {
        log.info("placeMarketOrder: инициируем рыночный ордер для chatId={}, symbol={}, quantity={}", chatId, symbol, quantity);
        String path = "/api/v3/order";
        long ts = Instant.now().toEpochMilli();

        Map<String,String> params = new LinkedHashMap<>();
        params.put("symbol",    symbol.toUpperCase());
        params.put("side",      "BUY");
        params.put("type",      "MARKET");
        params.put("quantity",  String.valueOf(quantity));
        params.put("timestamp", String.valueOf(ts));

        log.debug("placeMarketOrder: параметры запроса для chatId={} : {}", chatId, params);
        sendSignedRequest(chatId, path, params);
    }

    @Override
    public void placeOcoOrder(Long chatId, String symbol, double quantity, double stopPrice, double limitPrice) {
        log.info("placeOcoOrder: инициируем OCO ордер для chatId={}, symbol={}, quantity={}, stopPrice={}, limitPrice={}",
                chatId, symbol, quantity, stopPrice, limitPrice);
        String path = "/api/v3/order/oco";
        long ts = Instant.now().toEpochMilli();

        Map<String,String> params = new LinkedHashMap<>();
        params.put("symbol",    symbol.toUpperCase());
        params.put("side",      "BUY");
        params.put("quantity",  String.valueOf(quantity));
        params.put("stopPrice", String.valueOf(stopPrice));
        params.put("price",     String.valueOf(limitPrice));
        params.put("timestamp", String.valueOf(ts));

        log.debug("placeOcoOrder: параметры запроса для chatId={} : {}", chatId, params);
        sendSignedRequest(chatId, path, params);
    }

    private void sendSignedRequest(Long chatId, String path, Map<String,String> params) {
        try {
            // Собираем query-string
            String queryString = params.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("&"));

            // Подпись
            String signature = HmacSHA256Signer.sign(queryString, secretKey(chatId));
            String fullQS = queryString + "&signature=" + signature;

            String url = base(chatId) + path + "?" + fullQS;
            log.debug("sendSignedRequest: полная ссылка для chatId={} : {}", chatId, url);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-MBX-APIKEY", apiKey(chatId))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            log.debug("sendSignedRequest: отправляем HTTP POST для chatId={} по пути {}", chatId, path);
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            log.info("Binance order [{} {}] → {} {}", chatId, path, resp.statusCode(), resp.body());
        } catch (Exception e) {
            log.error("Error sending signed request to Binance for chatId={}", chatId, e);
            throw new RuntimeException(e);
        }
    }
}
