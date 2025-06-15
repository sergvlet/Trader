package com.chicu.trader.trading.service.binance;

import com.chicu.trader.trading.model.Candle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HttpBinanceCandleService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Candle> fetchCandles(String symbol, Duration timeframe, int limit) {
        String interval = convertToBinanceInterval(timeframe);
        String url = String.format(
                "https://api.binance.com/api/v3/klines?symbol=%s&interval=%s&limit=%d",
                symbol, interval, limit
        );

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            List<Candle> candles = new ArrayList<>();

            for (JsonNode array : root) {
                Instant openTime = Instant.ofEpochMilli(array.get(0).asLong());
                Instant closeTime = Instant.ofEpochMilli(array.get(6).asLong());

                double open = array.get(1).asDouble();
                double high = array.get(2).asDouble();
                double low  = array.get(3).asDouble();
                double close = array.get(4).asDouble();
                double volume = array.get(5).asDouble();

                Candle candle = Candle.builder()
                        .symbol(symbol)
                        .openTime(openTime.toEpochMilli())
                        .open(open)
                        .high(high)
                        .low(low)
                        .close(close)
                        .volume(volume)
                        .closeTime(closeTime.toEpochMilli())
                        .build();

                candles.add(candle);
            }
            return candles;

        } catch (Exception e) {
            log.error("Ошибка загрузки свечей с Binance: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private String convertToBinanceInterval(Duration duration) {
        long minutes = duration.toMinutes();

        if (minutes < 60) {
            return minutes + "m";
        } else if (minutes % 60 == 0 && minutes < 24 * 60) {
            return (minutes / 60) + "h";
        } else if (minutes % (24 * 60) == 0) {
            return (minutes / (24 * 60)) + "d";
        }
        // Default fallback
        return "1m";
    }
}
