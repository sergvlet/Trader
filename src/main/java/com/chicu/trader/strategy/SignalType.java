package com.chicu.trader.strategy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Глобальный тип сигнала – дальше его будет читать слой торговли.
 */
@Getter
@RequiredArgsConstructor
public enum SignalType {
    BUY("BUY"),
    SELL("SELL"),
    HOLD("HOLD");

    private final String label;
}
