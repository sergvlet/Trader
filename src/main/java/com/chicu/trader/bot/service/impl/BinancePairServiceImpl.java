package com.chicu.trader.bot.service.impl;

import com.chicu.trader.bot.service.BinancePairService;
import com.chicu.trader.dto.BinancePairDto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BinancePairServiceImpl implements BinancePairService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public List<BinancePairDto> getAllAvailablePairs(boolean isTestnet) {
        String baseUrl = isTestnet
                ? "https://testnet.binance.vision/api/v3/ticker/24hr"
                : "https://api.binance.com/api/v3/ticker/24hr";

        try {
            BinanceTicker[] response = restTemplate.getForObject(baseUrl, BinanceTicker[].class);
            if (response == null) return Collections.emptyList();

            return Arrays.stream(response)
                    .filter(t -> t.getSymbol().endsWith("USDT"))
                    .map(t -> BinancePairDto.builder()
                            .symbol(t.getSymbol())
                            .price(Double.parseDouble(t.getLastPrice()))
                            .priceChange(Double.parseDouble(t.getPriceChangePercent()))
                            .build())
                    .sorted(Comparator.comparingDouble(BinancePairDto::getPriceChange).reversed())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Ошибка при получении списка пар с Binance", e);
            return Collections.emptyList();
        }
    }

    private static class BinanceTicker {
        private String symbol;
        private String lastPrice;
        private String priceChangePercent;

        public String getSymbol() { return symbol; }
        public String getLastPrice() { return lastPrice; }
        public String getPriceChangePercent() { return priceChangePercent; }
    }
}
