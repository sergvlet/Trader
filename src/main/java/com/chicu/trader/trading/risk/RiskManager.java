// src/main/java/com/chicu/trader/trading/risk/RiskManager.java
package com.chicu.trader.trading.risk;

import java.math.BigDecimal;

/**
 * Рассчитывает объём позиции и тримминг в зависимости от рисковых параметров.
 */
public interface RiskManager {

    /**
     * Вычисляет объём лота (qty) для входа в позицию, а также
     * максимально допустимый убыток по этой позиции.
     *
     * @param freeBalanceUsd    свободный баланс в USD (или базовой валюте)
     * @param atrPct            волатильность в % (ATR)
     * @param riskPct           процент риска от баланса (например, 1.0 = 1%)
     * @param maxDrawdownPct    максимально допустимая просадка портфеля (например, 5.0 = 5%)
     * @param entryPrice        цена входа в позицию
     * @return результат позиционирования: qty и maxLossUsd
     */
    PositionSizingResult sizePosition(
        BigDecimal freeBalanceUsd,
        BigDecimal atrPct,
        BigDecimal riskPct,
        BigDecimal maxDrawdownPct,
        BigDecimal entryPrice
    );

    /**
     * Вычисляет, какую долю (0–1) позиции нужно закрыть,
     * если текущий убыток currentDrawdownUsd достиг maxDrawdownUsd.
     */
    BigDecimal trimmingFraction(
        BigDecimal currentDrawdownUsd,
        BigDecimal maxDrawdownUsd
    );

    class PositionSizingResult {
        private final BigDecimal quantity;
        private final BigDecimal maxLossUsd;

        public PositionSizingResult(BigDecimal quantity, BigDecimal maxLossUsd) {
            this.quantity = quantity;
            this.maxLossUsd = maxLossUsd;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public BigDecimal getMaxLossUsd() {
            return maxLossUsd;
        }
    }
}
