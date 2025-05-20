// src/main/java/com/chicu/trader/trading/service/binance/HttpBinanceSignedService.java
package com.chicu.trader.trading.service.binance;

import com.chicu.trader.bot.entity.UserSettings;
import com.chicu.trader.bot.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class HttpBinanceSignedService {

    private static final String PROD_REST = "https://api.binance.com";
    private static final String TEST_REST = "https://testnet.binance.vision";

    private final UserSettingsRepository userSettingsRepository;
    private final HttpClient            client = HttpClient.newHttpClient();

    /**
     * Возвращает JSON с информацией по аккаунту (балансы и т.д.).
     */
    public String getAccountInfo(Long chatId) {
        UserSettings us = userSettingsRepository.findById(chatId)
                .orElseThrow(() -> new IllegalStateException("UserSettings not found: " + chatId));

        String base = "REAL".equalsIgnoreCase(us.getMode()) ? PROD_REST : TEST_REST;
        String apiKey, secret;
        if ("REAL".equalsIgnoreCase(us.getMode())) {
            apiKey = us.getRealApiKey();
            secret = us.getRealSecretKey();
        } else {
            apiKey = us.getTestApiKey();
            secret = us.getTestSecretKey();
        }

        long ts = System.currentTimeMillis();
        String payload = "timestamp=" + ts;
        String signature = HmacSHA256Signer.sign(payload, secret);
        String url = String.format("%s/api/v3/account?%s&signature=%s", base, payload, signature);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("X-MBX-APIKEY", apiKey)
                .GET()
                .build();

        try {
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.body();
        } catch (Exception e) {
            log.error("Ошибка приватного запроса accountInfo: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
