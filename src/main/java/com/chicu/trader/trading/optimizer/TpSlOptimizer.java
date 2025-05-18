// src/main/java/com/chicu/trader/trading/optimizer/TpSlOptimizer.java
package com.chicu.trader.trading.optimizer;

import com.chicu.trader.trading.model.Candle;

import java.util.List;

public interface TpSlOptimizer {
    Result optimize(List<Candle> history);

    class Result {
        private final double tpPct;
        private final double slPct;
        private final double profit;

        public Result(double tpPct, double slPct, double profit) {
            this.tpPct = tpPct;
            this.slPct = slPct;
            this.profit = profit;
        }
        public double getTpPct()  { return tpPct; }
        public double getSlPct()  { return slPct; }
        public double getProfit() { return profit; }
    }
}
