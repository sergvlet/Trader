// src/main/java/com/chicu/trader/trading/CandleService.java
package com.chicu.trader.trading;

import com.chicu.trader.model.ProfitablePair;
import com.chicu.trader.trading.model.Candle;
import reactor.core.publisher.Flux;

import java.util.List;

public interface CandleService {
    /** Паблишит новые часовые свечи для заданного пользователя и списка пар */
    Flux<Candle> streamHourly(Long chatId, List<ProfitablePair> pairs);

    /** Берёт историю часовых свечей по символу для пользователя */
    List<Candle> historyHourly(Long chatId, String symbol, int count);

    /** Берёт историю 4-часовых свечей по символу для пользователя */
    List<Candle> history4h(Long chatId, String symbol, int count);
}
