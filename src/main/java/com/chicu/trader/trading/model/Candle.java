// src/main/java/com/chicu/trader/trading/model/Candle.java
package com.chicu.trader.trading.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Candle {
    private String symbol;
    private long openTime;
    private double open;
    private double high;
    private double low;
    private double close;
    private double volume;
    private long closeTime;

    /** Из WebSocket-события: event.get("s") — symbol. */
    @SuppressWarnings("unchecked")
    public static Candle fromEvent(Map<String, Object> event) {
        String symbol = (String) event.get("s");
        Map<String, Object> k = (Map<String, Object>) event.get("k");
        long t = ((Number) k.get("t")).longValue();
        double o = Double.parseDouble((String) k.get("o"));
        double h = Double.parseDouble((String) k.get("h"));
        double l = Double.parseDouble((String) k.get("l"));
        double c = Double.parseDouble((String) k.get("c"));
        double v = Double.parseDouble((String) k.get("v"));
        long T = ((Number) k.get("T")).longValue();
        return new Candle(symbol, t, o, h, l, c, v, T);
    }

    /** Из REST-ответа klines (каждый элемент — List<Object>) и передаём symbol. */
    @SuppressWarnings("unchecked")
    public static List<Candle> fromKlines(List<List<Object>> rawNested, String symbol) {
        return ((List<List<Object>>) (List<?>) rawNested).stream()
                .map(r -> new Candle(
                        symbol,
                        ((Number) r.get(0)).longValue(),
                        Double.parseDouble((String) r.get(1)),
                        Double.parseDouble((String) r.get(2)),
                        Double.parseDouble((String) r.get(3)),
                        Double.parseDouble((String) r.get(4)),
                        Double.parseDouble((String) r.get(5)),
                        ((Number) r.get(6)).longValue()
                ))
                .toList();
    }
}
