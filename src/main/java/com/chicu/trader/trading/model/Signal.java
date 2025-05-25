package com.chicu.trader.trading.model;

import com.chicu.trader.model.SignalType;
import lombok.Builder;

/**
 * Структура сигнала, который возвращает стратегия:
 * - тип сигнала (BUY, SELL, HOLD)
 * - причина (опционально)
 */
@Builder
public record Signal(SignalType type, String reason) {
}
