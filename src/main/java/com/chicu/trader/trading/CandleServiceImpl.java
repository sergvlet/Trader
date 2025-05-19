// src/main/java/com/chicu/trader/trading/CandleServiceImpl.java
package com.chicu.trader.trading;

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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandleServiceImpl implements CandleService {

    private final BinanceClientProvider clientProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // карта для тестовых «заглушек»
    private final Map<Long, List<Candle>> overrides = new ConcurrentHashMap<>();

    @Override
    public Flux<Candle> streamHourly(Long chatId, List<ProfitablePair> pairs) {
        if (overrides.containsKey(chatId)) {
            List<Candle> list = overrides.remove(chatId);
            return Flux.fromIterable(list);
        }
        return Flux
            .interval(Duration.ZERO, Duration.ofHours(1))
            .flatMapIterable(tick -> pairs.stream()
                .map(p -> fetchLastCandle(chatId, p.getSymbol()))
                .collect(Collectors.toList())
            );
    }

    @Override
    public void setStreamOverride(Long chatId, List<Candle> override) {
        overrides.put(chatId, override);
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

    @SuppressWarnings("unchecked")
    private List<Candle> parseKlines(Long chatId, String symbol, String interval, int limit) {
        var rest = clientProvider.restClient(chatId);
        var params = new LinkedHashMap<String, Object>();
        params.put("symbol", symbol);
        params.put("interval", interval);
        params.put("limit", limit);

        String json = rest.createMarket().klines(params);
        try {
            // парсим в List<List<Object>>
            List<List<Object>> rawNested = objectMapper.readValue(
                json, new TypeReference<List<List<Object>>>() {}
            );
            // передаём «сырые» клайны в модель
            return Candle.fromKlines(rawNested, symbol);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse klines JSON for " + symbol, e);
        }
    }
}
