// src/main/java/com/chicu/trader/trading/risk/DefaultRiskManager.java
package com.chicu.trader.trading.risk;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DefaultRiskManager: фикс-фракционный метод управления риском.
 * Покупает riskPct% от свободного баланса, рассчитывает SL как ATR*entryPrice.
 */
@Component
public class DefaultRiskManager implements RiskManager {

    private static final int SCALE = 8;     // точность для quantity
    private static final int PCT_SCALE = 4; // точность для процентов

    @Override
    public PositionSizingResult sizePosition(
            BigDecimal freeBalanceUsd,
            BigDecimal atrPct,
            BigDecimal riskPct,
            BigDecimal maxDrawdownPct,
            BigDecimal entryPrice
    ) {
        // 1) riskUsd = freeBalanceUsd * (riskPct / 100)
        BigDecimal riskUsd = freeBalanceUsd
            .multiply(riskPct.divide(BigDecimal.valueOf(100), PCT_SCALE, RoundingMode.HALF_UP));

        // 2) дистанция стоп-лосса = atrPct% от entryPrice
        BigDecimal slDistanceUsd = entryPrice
            .multiply(atrPct.divide(BigDecimal.valueOf(100), PCT_SCALE, RoundingMode.HALF_UP));

        // 3) qty = riskUsd / slDistanceUsd
        BigDecimal qty = riskUsd.divide(slDistanceUsd, SCALE, RoundingMode.HALF_UP);

        // 4) maxDrawdownUsd = freeBalanceUsd * (maxDrawdownPct / 100)
        BigDecimal maxDrawdownUsd = freeBalanceUsd
            .multiply(maxDrawdownPct.divide(BigDecimal.valueOf(100), PCT_SCALE, RoundingMode.HALF_UP));

        return new PositionSizingResult(qty, maxDrawdownUsd);
    }

    @Override
    public BigDecimal trimmingFraction(BigDecimal currentDrawdownUsd, BigDecimal maxDrawdownUsd) {
        BigDecimal absDraw = currentDrawdownUsd.abs();
        if (absDraw.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal frac = absDraw
            .divide(maxDrawdownUsd, SCALE, RoundingMode.HALF_UP);
        return frac.min(BigDecimal.ONE);
    }
}
