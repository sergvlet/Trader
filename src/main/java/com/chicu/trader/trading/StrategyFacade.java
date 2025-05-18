// src/main/java/com/chicu/trader/trading/StrategyFacade.java
package com.chicu.trader.trading;

import com.chicu.trader.model.ProfitablePair;
import com.chicu.trader.repository.ProfitablePairRepository;
import com.chicu.trader.trading.context.StrategyContext;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.ml.MlSignalFilter;
import com.chicu.trader.trading.indicator.IndicatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StrategyFacade {

    private final ProfitablePairRepository pairRepo;
    private final CandleService candleService;
    private final IndicatorService indicators;
    private final MlSignalFilter mlFilter;
    private final BalanceService balanceService;
    private final PositionService positionService;

    /**
     * Загружает ONNX-модель (вызываем один раз перед стримом).
     */
    public void loadModel() {
        mlFilter.loadModel("models/ml_signal_filter.onnx");
    }

    /**
     * Строит контекст стратегии для данной свечи.
     */
    public StrategyContext buildContext(Long chatId, Candle candle, List<ProfitablePair> pairs) {
        return new StrategyContext(
                chatId,
                candle,
                pairs,
                candleService,
                indicators,
                mlFilter,
                balanceService,
                positionService
        );
    }

    /**
     * Решает, нужно ли войти в позицию.
     */
    public boolean shouldEnter(StrategyContext ctx) {
        return ctx.passesMlFilter()
                && ctx.passesVolume()
                && ctx.passesMultiTimeframe()
                && ctx.passesRsiBb();
    }
}
