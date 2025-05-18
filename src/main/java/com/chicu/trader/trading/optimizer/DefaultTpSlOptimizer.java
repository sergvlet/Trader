// src/main/java/com/chicu/trader/trading/optimizer/DefaultTpSlOptimizer.java
package com.chicu.trader.trading.optimizer;

import com.chicu.trader.trading.model.Candle;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Основная (primary) реализация TpSlOptimizer.
 */
@Service
@Primary
public class DefaultTpSlOptimizer implements TpSlOptimizer {

    @Override
    public Result optimize(List<Candle> history) {
        // TODO: ваша логика оптимизации TP/SL
        double defaultTp = 0.03;
        double defaultSl = 0.01;
        return new Result(defaultTp, defaultSl, 0.0);
    }
}
