// src/main/java/com/chicu/trader/trading/service/StrategyEngine.java
package com.chicu.trader.trading.service;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.model.SignalType;
import com.chicu.trader.strategy.StrategyRegistry;
import com.chicu.trader.strategy.TradeStrategy;
import com.chicu.trader.strategy.StrategyType;
import com.chicu.trader.trading.model.Candle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StrategyEngine {

    private final StrategyRegistry registry;

    /**
     * Вычисляет глобальный сигнал для пользователя на основе выбранной стратегии.
     *
     * @param chatId   идентификатор чата/пользователя
     * @param candles  список исторических свечей
     * @param settings настройки AI-торговли (содержат StrategyType)
     * @return SignalType.BUY / SELL / HOLD
     */
    public SignalType evaluate(Long chatId, List<Candle> candles, AiTradingSettings settings) {
        // 1) Получаем enum-тип стратегии
        StrategyType strategyType = settings.getStrategy();

        // 2) Из registry берём бин нужной стратегии
        TradeStrategy strat = registry.get(strategyType);

        // 3) Вызываем локальный evaluate
        TradeStrategy.SignalType localSignal = strat.evaluate(candles, settings);

        // 4) Маппим в ваш глобальный SignalType
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
