// src/main/java/com/chicu/trader/strategy/StrategyService.java
package com.chicu.trader.strategy;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.model.SignalType;
import com.chicu.trader.trading.model.Candle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StrategyService {

    private final StrategyRegistry registry;

    /**
     * Вычисляет торговый сигнал для пользователя.
     *
     * @param chatId   идентификатор чата/пользователя
     * @param candles  список исторических свечей (последний бар — текущий)
     * @param settings настройки AI-торговли пользователя
     * @return сигнал BUY, SELL или HOLD
     */
    public SignalType evaluate(Long chatId, List<Candle> candles, AiTradingSettings settings) {
        // 1) Получаем enum-тип стратегии
        StrategyType strategyType = settings.getStrategy();

        // 2) Из registry достаём бин нужной стратегии
        TradeStrategy strategy = registry.get(strategyType);

        // 3) Вычисляем локальный сигнал стратегии
        TradeStrategy.SignalType localSignal = strategy.evaluate(candles, settings);

        // 4) Маппим в глобальный SignalType
        switch (localSignal) {
            case BUY:
                return SignalType.BUY;
            case SELL:
                return SignalType.SELL;
            default:
                return SignalType.HOLD;
        }
    }
}
