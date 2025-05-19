// src/main/java/com/chicu/trader/trading/CandleService.java
package com.chicu.trader.trading;

import com.chicu.trader.model.ProfitablePair;
import com.chicu.trader.trading.model.Candle;
import reactor.core.publisher.Flux;

import java.util.List;

public interface CandleService {
    Flux<Candle> streamHourly(Long chatId, List<ProfitablePair> pairs);
    List<Candle> historyHourly(Long chatId, String symbol, int count);
    List<Candle> history4h(Long chatId, String symbol, int count);

    // Для тестов: позволяет подменить к какому потоку будет привязан chatId
    void setStreamOverride(Long chatId, List<Candle> override);
}
