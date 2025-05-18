// src/main/java/com/chicu/trader/trading/CandleServiceImpl.java
package com.chicu.trader.trading;

import com.chicu.trader.model.ProfitablePair;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.binance.BinanceClientProvider;
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
    private final ObjectMapper          objectMapper = new ObjectMapper();

    @Override
    public Flux<Candle> streamHourly(Long chatId, List<ProfitablePair> pairs) {
        return Flux.interval(Duration.ZERO, Duration.ofHours(1))
            .flatMapIterable(tick ->
                pairs.stream()
                     .map(p -> fetchLastCandle(chatId, p.getSymbol()))
                     .collect(Collectors.toList())
            )
            .filter(c -> c != null);
    }

    private Candle fetchLastCandle(Long chatId, String symbol) {
        List<Candle> list = parseKlines(chatId, symbol, "1h", 1);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<Candle> historyHourly(Long chatId, String symbol, int limit) {
        return parseKlines(chatId, symbol, "1h", limit);
    }

    @Override
    public List<Candle> history4h(Long chatId, String symbol, int limit) {
        return parseKlines(chatId, symbol, "4h", limit);
    }

    @SuppressWarnings("unchecked")
    private List<Candle> parseKlines(Long chatId, String symbol, String interval, int limit) {
        var rest = clientProvider.restClient(chatId);
        var params = new LinkedHashMap<String, Object>();
        params.put("symbol",   symbol);
        params.put("interval", interval);
        params.put("limit",    limit);

        String json = rest.createMarket().klines(params);
        try {
            List<List<Object>> rawNested = objectMapper.readValue(
                json, new TypeReference<List<List<Object>>>() {}
            );
            return Candle.fromKlines((List<Object>)(List<?>) rawNested, symbol);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to parse klines for " + symbol, ex);
        }
    }
}
