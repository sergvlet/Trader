// src/main/java/com/chicu/trader/trading/optimizer/TpSlOptimizerImpl.java
package com.chicu.trader.trading.optimizer;

import com.chicu.trader.trading.optimizer.TpSlOptimizer.Result;
import com.chicu.trader.trading.model.Candle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TpSlOptimizerImpl implements TpSlOptimizer {

    // Внедряйте сюда, если нужны дополнительные сервисы
    // private final CandleService candleService; 

    @Override
    public Result optimize(Long chatId) {
        // получите историю через сигнал сервис, или вручную
        List<Candle> history = List.of(); // замените на реальную загрузку
        return optimize(history);
    }

    @Override
    public Result optimize(List<Candle> history) {
        // Ваша логика перебора TP/SL по history
        double bestTp = 0.03, bestSl = 0.01, profit = 0;
        // …
        return new Result(bestTp, bestSl, profit);
    }
}
