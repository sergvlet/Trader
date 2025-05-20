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
        return userSettingsService.isTestnet(chatId) ? TEST_REST : PROD_REST;
    }

    private String apiKey(Long chatId) {
        return userSettingsService.getApiCredentials(chatId).getApiKey();
    }

    private String secretKey(Long chatId) {
        return userSettingsService.getApiCredentials(chatId).getSecretKey();
    }

    @Override
    public void placeMarketOrder(Long chatId, String symbol, double quantity) {
        String path = "/api/v3/order";
        long ts = Instant.now().toEpochMilli();

        Map<String,String> params = new LinkedHashMap<>();
        params.put("symbol",    symbol.toUpperCase());
        params.put("side",      "BUY");
        params.put("type",      "MARKET");
        params.put("quantity",  String.valueOf(quantity));
        params.put("timestamp", String.valueOf(ts));

        sendSignedRequest(chatId, path, params);
    }

    @Override
    public void placeOcoOrder(Long chatId, String symbol, double quantity, double stopPrice, double limitPrice) {
        String path = "/api/v3/order/oco";
        long ts = Instant.now().toEpochMilli();

        Map<String,String> params = new LinkedHashMap<>();
        params.put("symbol",    symbol.toUpperCase());
        params.put("side",      "BUY");
        params.put("quantity",  String.valueOf(quantity));
        params.put("stopPrice", String.valueOf(stopPrice));
        params.put("price",     String.valueOf(limitPrice));
        params.put("timestamp", String.valueOf(ts));

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

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(base(chatId) + path + "?" + fullQS))
                    .header("X-MBX-APIKEY", apiKey(chatId))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            log.info("Binance order [{} {}] → {} {}",
                    chatId, path, resp.statusCode(), resp.body());
        } catch (Exception e) {
            log.error("Error sending signed request to Binance", e);
            throw new RuntimeException(e);
        }
    }
}
