// src/main/java/com/chicu/trader/strategy/scalping/ScalpingStrategy.java
package com.chicu.trader.strategy.scalping;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.strategy.TradeStrategy;
import com.chicu.trader.strategy.StrategyType;
import com.chicu.trader.trading.model.Candle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Заготовка под Скальпинг-стратегию:
 * минимальная демонстрация – никогда ничего не делает.
 */
@Component
@RequiredArgsConstructor
public class ScalpingStrategy implements TradeStrategy {

    @Override
    public StrategyType getType() {
        return StrategyType.SCALPING;
    }

    @Override
    public SignalType evaluate(List<Candle> candles, AiTradingSettings settings) {
        // TODO: Ваша логика скальпинга
        return SignalType.HOLD;
    }
}
