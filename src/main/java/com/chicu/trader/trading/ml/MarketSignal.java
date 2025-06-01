package com.chicu.trader.trading.ml;

/**
 * Возможные сигналы, которые может выдать ML‐фильтр.
 */
public enum MarketSignal {
    /** Вход в лонг (BUY) */
    BUY,
    /** Выход (SELL) */
    SELL,
    /** Пропустить, не участвовать в торговле */
    SKIP
}
