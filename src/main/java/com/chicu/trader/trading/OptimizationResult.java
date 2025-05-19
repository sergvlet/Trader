// src/main/java/com/chicu/trader/trading/OptimizationResult.java
package com.chicu.trader.trading;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OptimizationResult {
    private final double tp;
    private final double sl;
    private final int topN;
    private final List<String> symbols;
    private final String timeframe;
    private final double riskThreshold;
    private final double maxDrawdown;

    // Добавленные поля для расширенной оптимизации
    private final Integer leverage;
    private final Integer maxPositions;
    private final Integer tradeCooldown;
    private final Double slippageTolerance;
    private final String orderType;
    private final Boolean notificationsEnabled;
    private final String modelVersion;

    /** Возвращает JSON-представление TP/SL (оставляем без изменений) */
    public String toJson() {
        return tp + "," + sl;
    }
}
