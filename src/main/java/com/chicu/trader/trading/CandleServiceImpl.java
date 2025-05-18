// src/main/java/com/chicu/trader/trading/CandleServiceImpl.java
package com.chicu.trader.trading;

import com.binance.connector.client.impl.SpotClientImpl;
import com.chicu.trader.model.ProfitablePair;
import com.chicu.trader.trading.binance.BinanceClientProvider;
import com.chicu.trader.trading.model.Candle;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandleServiceImpl implements CandleService {

    private final BinanceClientProvider clientProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Flux<Candle> streamHourly(Long chatId, List<ProfitablePair> pairs) {
        return Flux
            .interval(Duration.ZERO, Duration.ofHours(1))
            .flatMapIterable(tick -> pairs.stream()
                .map(p -> fetchLastCandle(chatId, p.getSymbol()))
                .collect(Collectors.toList())
            );
    }

    private Candle fetchLastCandle(Long chatId, String symbol) {
        List<Candle> list = parseKlines(chatId, symbol, "1h", 1);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<Candle> historyHourly(Long chatId, String symbol, int count) {
        return parseKlines(chatId, symbol, "1h", count);
    }

    @Override
    public List<Candle> history4h(Long chatId, String symbol, int count) {
        return parseKlines(chatId, symbol, "4h", count);
    }

    private List<Candle> parseKlines(Long chatId, String symbol, String interval, int limit) {
        SpotClientImpl rest = clientProvider.restClient(chatId);

        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("symbol", symbol);
        params.put("interval", interval);
        params.put("limit", limit);

        String json = rest.createMarket().klines(params);
        try {
            // Парсим в вложенный список свечей
            List<List<Object>> rawNested = objectMapper.readValue(
                json, new TypeReference<List<List<Object>>>() {}
            );
            // Конвертим и передаём symbol
            return Candle.fromKlines((List<Object>)(List<?>) rawNested, symbol);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse klines JSON for " + symbol, e);
        }
    }
}
