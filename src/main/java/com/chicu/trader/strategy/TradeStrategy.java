package com.chicu.trader.strategy;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.strategy.SignalType;
import java.util.List;

public interface TradeStrategy {
    SignalType evaluate(List<Candle> candles, AiTradingSettings settings);
    StrategyType getType();

    default void train(Long chatId) {}
    default boolean isTrainable() { return false; }
}
