package com.chicu.trader.strategy;

import com.chicu.trader.trading.model.Candle;

import java.util.List;

public interface TradeStrategy {

    StrategyType getType();

    SignalType evaluate(List<Candle> candles, StrategySettings settings);

    StrategySettings getSettings(Long chatId);

    default boolean isTrainable() {
        return false;
    }

    default void train(Long chatId) {
        // По умолчанию ничего не делает
    }
}
