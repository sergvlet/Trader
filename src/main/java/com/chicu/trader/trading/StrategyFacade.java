// src/main/java/com/chicu/trader/trading/StrategyFacade.java
package com.chicu.trader.trading;

import com.chicu.trader.model.ProfitablePair;
import com.chicu.trader.trading.context.StrategyContext;
import com.chicu.trader.trading.indicator.IndicatorService;
import com.chicu.trader.trading.ml.MlSignalFilter;
import com.chicu.trader.trading.model.Candle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StrategyFacade {
    private final CandleService      candleService;
    private final IndicatorService indicators;
    private final MlSignalFilter mlFilter;

    /**
     * Строит контекст стратегии для данной свечи и списка символов.
     */
    public StrategyContext buildContext(Long chatId,
                                        Candle candle,
                                        List<ProfitablePair> symbols) {
        return new StrategyContext(
            chatId,
            candle,
            symbols,           // теперь список символов
            candleService,     // инжектите его в этот класс
            indicators,
            mlFilter
        );
    }

    /**
     * Проверяет все условия входа.
     */
    public boolean shouldEnter(StrategyContext ctx) {
        return ctx.passesMlFilter()
            && ctx.passesVolume()
            && ctx.passesMultiTimeframe()
            && ctx.passesRsiBb();
    }
}
