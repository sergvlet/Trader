package com.chicu.trader.strategy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum StrategyType {

    RSI_EMA("RSI + EMA"),
    SCALPING("Скальпинг"),
    ML_MODEL("ML-модель"),
    SWING("Свинг стратегия"),
    DEFAULT("По умолчанию");

    private final String label;

    public static StrategyType findByCode(String code) {
        return Arrays.stream(values())
                .filter(e -> e.name().equalsIgnoreCase(code))
                .findFirst()
                .orElse(DEFAULT);
    }
}
