// src/main/java/com/chicu/trader/trading/BinanceBalanceService.java
package com.chicu.trader.trading;

import com.chicu.trader.bot.entity.UserSettings;
import com.chicu.trader.bot.repository.UserSettingsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class BinanceBalanceService implements BalanceService {

    private final UserSettingsRepository settingsRepo;
    private final ObjectMapper mapper = new ObjectMapper();
    private final WebClient webClient = WebClient.builder()
        .baseUrl("https://api.binance.com")
        .build();

    @Override
    public double getAvailableUsdt(Long chatId) {
        UserSettings s = settingsRepo.findById(chatId)
            .orElseThrow(() -> new IllegalStateException("No settings for user " + chatId));

        String apiKey = "REAL".equalsIgnoreCase(s.getMode()) ? s.getRealApiKey() : s.getTestApiKey();
        String secret = "REAL".equalsIgnoreCase(s.getMode()) ? s.getRealSecretKey() : s.getTestSecretKey();

        long timestamp = System.currentTimeMillis();
        String query = "timestamp=" + timestamp;
        String signature = hmacSha256(secret, query);
        String uri = "/api/v3/account?" + query + "&signature=" + signature;

        String json = webClient.get()
            .uri(uri)
            .header("X-MBX-APIKEY", apiKey)
            .retrieve()
            .bodyToMono(String.class)
            .block();

        try {
            JsonNode root = mapper.readTree(json);
            for (JsonNode asset : root.get("balances")) {
                if ("USDT".equals(asset.get("asset").asText())) {
                    return asset.get("free").asDouble();
                }
            }
            throw new IllegalStateException("USDT balance not found");
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch balances", e);
        }
    }

    private String hmacSha256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signatureBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            var sb = new StringBuilder();
            for (byte b : signatureBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Unable to sign request", ex);
        }
    }
}
