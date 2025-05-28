// src/main/java/com/chicu/trader/strategy/TradeStrategy.java
package com.chicu.trader.strategy;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.trading.model.Candle;

import java.util.List;

/**
 * Интерфейс для любой торговой стратегии.
 */
public interface TradeStrategy {

    /** Тип стратегии — соответствует StrategyType. */
    StrategyType getType();

    /**
     * Основной метод — по списку свечей и настройкам
     * возвращает BUY, SELL или HOLD.
     *
     * @param candles  последние N свечей (символ внутри Candle)
     * @param settings настройки пользователя (TP/SL, риск, другие параметры)
     */
    SignalType evaluate(List<Candle> candles, AiTradingSettings settings);

    enum SignalType { BUY, SELL, HOLD }
}
