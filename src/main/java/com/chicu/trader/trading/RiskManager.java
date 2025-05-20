// src/main/java/com/chicu/trader/trading/risk/RiskManager.java
package com.chicu.trader.trading;

import java.math.BigDecimal;

/**
 * Рассчитывает объём позиции и тримминг в зависимости от рисковых параметров.
 */
public interface RiskManager {

    /**
     * Вычисляет объём лота (qty) для входа, исходя из доступного баланса в USD,
     * порога риска (riskPct, например 1.0 = 1%), текущей волатильности (ATR-процент)
     * и максимальной просадки (максимальный drawdown, 5.0 = 5%).
     *
     * @param freeBalanceUsd  сумма свободного баланса в USD
     * @param atrPct          волатильность (0.02 = 2% ATR)
     * @param riskPct         процент риска на сделку (1.0 = 1%)
     * @param maxDrawdownPct  максимальная просадка портфеля (5.0 = 5%)
     * @param entryPrice      текущая цена актива
     * @return расчёт результата: qty и максимально допустимый loss в USD
     */
    PositionSizingResult sizePosition(
        BigDecimal freeBalanceUsd,
        BigDecimal atrPct,
        BigDecimal riskPct,
        BigDecimal maxDrawdownPct,
        BigDecimal entryPrice
    );

    /**
     * Если текущее значение PnL (в USD) ниже допустимого (maxDrawdownUsd),
     * возвращает, какую долю (0-1) позиции нужно закрыть.
     *
     * @param currentDrawdownUsd  уже зафиксированный убыток (USD, отрицательный)
     * @param maxDrawdownUsd      максимально допустимый убыток (USD, положительный)
     * @return доля позиции к закрытию (0.0–1.0)
     */
    BigDecimal trimmingFraction(BigDecimal currentDrawdownUsd, BigDecimal maxDrawdownUsd);

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
