// src/main/java/com/chicu/trader/trading/risk/DefaultRiskManager.java
package com.chicu.trader.trading;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class DefaultRiskManager implements RiskManager {

    private static final int SCALE = 8;      // точность для quantity
    private static final int PCT_SCALE = 4;  // точность для процентов

    @Override
    public PositionSizingResult sizePosition(
            BigDecimal freeBalanceUsd,
            BigDecimal atrPct,
            BigDecimal riskPct,
            BigDecimal maxDrawdownPct,
            BigDecimal entryPrice
    ) {
        // 1) сколько USD рискуем: freeBalance * (riskPct / 100)
        BigDecimal riskUsd = freeBalanceUsd
                .multiply(riskPct.divide(BigDecimal.valueOf(100), PCT_SCALE, RoundingMode.HALF_UP));

        // 2) дистанция SL = atrPct * entryPrice
        BigDecimal slDistanceUsd = atrPct.multiply(entryPrice);

        // 3) quantity = riskUsd / slDistanceUsd
        BigDecimal qty = riskUsd
                .divide(slDistanceUsd, SCALE, RoundingMode.HALF_UP);

        // 4) максимальный loss всего портфеля
        BigDecimal maxDrawdownUsd = freeBalanceUsd
                .multiply(maxDrawdownPct.divide(BigDecimal.valueOf(100), PCT_SCALE, RoundingMode.HALF_UP));

        return new PositionSizingResult(qty, maxDrawdownUsd);
    }

    @Override
    public BigDecimal trimmingFraction(BigDecimal currentDrawdownUsd, BigDecimal maxDrawdownUsd) {
        // если текущий убыток = -500, maxDrawdown = 1000 => fraction = 0.5
        BigDecimal absDrawdown = currentDrawdownUsd.abs();
        if (absDrawdown.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal frac = absDrawdown
                .divide(maxDrawdownUsd, SCALE, RoundingMode.HALF_UP);
        return frac.min(BigDecimal.ONE);
    }
}
