// src/main/java/com/chicu/trader/trading/optimizer/TpSlResult.java
package com.chicu.trader.trading.optimizer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Результат оптимизации Take-Profit и Stop-Loss.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class TpSlResult {
    /** Процент Take-Profit, например 0.03 = 3% */
    private final double tpPct;
    /** Процент Stop-Loss, например 0.01 = 1% */
    private final double slPct;
    /** Ожидаемая прибыль (для оценки качества) */
    private final double profit;
}
