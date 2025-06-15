package com.chicu.trader.trading.service;

import com.chicu.trader.bot.service.UserSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceService {

    private final UserSettingsService userSettingsService;
    private final RestTemplate restTemplate = new RestTemplate();

    public BigDecimal getPrice(Long chatId, String symbol) {
        String baseUrl = userSettingsService.isTestnet(chatId)
                ? "https://testnet.binance.vision"
                : "https://api.binance.com";
        try {
            String url = baseUrl + "/api/v3/ticker/price?symbol=" + symbol;
            Map<?, ?> response = restTemplate.getForObject(url, Map.class);
            if (response == null || !response.containsKey("price")) return null;
            return new BigDecimal((String) response.get("price"));
        } catch (Exception e) {
            log.error("❌ Error fetching price for {} (chatId={}): {}", symbol, chatId, e.getMessage());
            return null;
        }
    }
}
