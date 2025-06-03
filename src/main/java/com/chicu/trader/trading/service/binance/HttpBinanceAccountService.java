package com.chicu.trader.trading.service.binance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HttpBinanceAccountService {

    private final HttpBinanceSignedService signedService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Возвращает карту доступных балансов (только free).
     */
    public Map<String, Double> getBalances(Long chatId) {
        try {
            String json = signedService.getAccountInfo(chatId);
            JsonNode root = objectMapper.readTree(json);
            JsonNode balances = root.get("balances");

            Map<String, Double> result = new HashMap<>();
            if (balances != null && balances.isArray()) {
                for (JsonNode b : balances) {
                    String asset = b.get("asset").asText();
                    double free = b.get("free").asDouble();
                    result.put(asset, free);
                }
            }
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при разборе балансов Binance: " + e.getMessage(), e);
        }
    }
}
