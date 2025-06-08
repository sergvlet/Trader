// src/main/java/com/chicu/trader/trading/service/AccountService.java
package com.chicu.trader.trading.service;

import com.chicu.trader.bot.service.UserSettingsService;
import com.chicu.trader.trading.service.binance.HmacSHA256Signer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class AccountService {

    private static final String PROD_REST = "https://api.binance.com";
    private static final String TEST_REST = "https://testnet.binance.vision";

    private final UserSettingsService userSettingsService;
    private final HttpClient         httpClient   = HttpClient.newHttpClient();
    private final ObjectMapper       objectMapper = new ObjectMapper();

    /**
     * Запрашивает текущее время сервера Binance.
     */
    private long getServerTime(boolean isTest) throws Exception {
        String base = isTest ? TEST_REST : PROD_REST;
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(base + "/api/v3/time"))
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new RuntimeException("Time API HTTP " + resp.statusCode() + ": " + resp.body());
        }
        JsonNode root = objectMapper.readTree(resp.body());
        return root.get("serverTime").asLong();
    }

    /**
     * Возвращает свободный баланс по asset (USDT, BUSD и т.п.).
     */
    public double getFreeBalance(Long chatId, String asset) {
        try {
            boolean isTest = userSettingsService.isTestnet(chatId);
            String base   = isTest ? TEST_REST : PROD_REST;
            String apiKey = userSettingsService.getApiCredentials(chatId).getApiKey();
            String secret = userSettingsService.getApiCredentials(chatId).getSecretKey();

            long ts = getServerTime(isTest);

            Map<String, String> params = new LinkedHashMap<>();
            // расширяем окно приёма до 60 секунд
            params.put("recvWindow", "60000");
            params.put("timestamp",  String.valueOf(ts));

            String qs = params.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("&"));
            String signature = HmacSHA256Signer.sign(qs, secret);
            String fullQs = qs + "&signature=" + signature;

            String url = base + "/api/v3/account?" + fullQs;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-MBX-APIKEY", apiKey)
                    .GET()
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new RuntimeException("Account API HTTP " + resp.statusCode() + ": " + resp.body());
            }

            JsonNode root = objectMapper.readTree(resp.body());
            for (JsonNode b : root.get("balances")) {
                if (asset.equalsIgnoreCase(b.get("asset").asText())) {
                    return b.get("free").asDouble();
                }
            }
            log.warn("AccountService ▶ asset {} not found, returning 0", asset);
            return 0.0;
        } catch (Exception e) {
            log.error("AccountService ▶ error fetching balance for {}: {}", asset, e.getMessage());
            return 0.0;
        }
    }
}
