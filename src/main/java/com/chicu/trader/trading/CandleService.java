// src/main/java/com/chicu/trader/trading/CandleService.java
package com.chicu.trader.trading;

import com.chicu.trader.model.ProfitablePair;
import com.chicu.trader.trading.model.Candle;
import reactor.core.publisher.Flux;

import java.util.List;

public interface CandleService {

    /** Живой поток часовых свечей по списку ProfitablePair */
    Flux<Candle> streamHourly(Long chatId, List<ProfitablePair> pairs);

    /** История часовых свечей для одного символа */
    List<Candle> historyHourly(Long chatId, String symbol, int limit);

    /** История 4-часовых свечей для одного символа */
    List<Candle> history4h(Long chatId, String symbol, int limit);
}
