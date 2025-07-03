package com.chicu.trader.trading.service;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.strategy.SignalType;
import com.chicu.trader.strategy.StrategyRegistry;
import com.chicu.trader.strategy.StrategySettings;
import com.chicu.trader.strategy.StrategyType;
import com.chicu.trader.strategy.TradeStrategy;
import com.chicu.trader.trading.model.Candle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
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
        StrategyType strategyType = settings.getStrategy();
        log.info("Запускаем стратегию {} для chatId={}", strategyType, chatId);

        TradeStrategy strat = registry.getStrategy(strategyType);
        StrategySettings strategySettings = strat.getSettings(chatId);

        SignalType signal = strat.evaluate(candles, strategySettings);
        return signal != null ? signal : SignalType.HOLD;
    }
}
