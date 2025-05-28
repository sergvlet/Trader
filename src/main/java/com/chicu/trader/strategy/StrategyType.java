// src/main/java/com/chicu/trader/strategy/StrategyType.java
package com.chicu.trader.strategy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * Коды всех поддерживаемых стратегий.
 */
@Getter
@RequiredArgsConstructor
public enum StrategyType {
    RSI_EMA("RSI + EMA"),
    SCALPING("Скальпинг"),
    ML_MODEL("ML-модель"),
    SWING("Свинг"),
    DEFAULT("По умолчанию");

    private final String label;

    /**
     * По коду (имени enum) находит стратегию, иначе возвращает DEFAULT.
     */
    public static StrategyType findByCode(String code) {
        return Arrays.stream(values())
                .filter(e -> e.name().equalsIgnoreCase(code))
                .findFirst()
                .orElse(DEFAULT);
    }
}
