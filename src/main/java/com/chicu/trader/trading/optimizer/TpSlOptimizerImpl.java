// src/main/java/com/chicu/trader/trading/optimizer/TpSlOptimizerImpl.java
package com.chicu.trader.trading.optimizer;

import com.chicu.trader.trading.model.Candle;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Вторая реализация, не-Primary.
 */
@Service
public class TpSlOptimizerImpl implements TpSlOptimizer {
    @Override
    public Result optimize(List<Candle> history) {
        // Другая логика...
        return new Result(0.05, 0.02, 0.0);
    }
}
