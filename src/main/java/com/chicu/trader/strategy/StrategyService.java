package com.chicu.trader.strategy;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.trading.model.Candle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис, который по списку свечей и настройкам пользователя
 * вычисляет глобальный SignalType (для торговли).
 */
@Service
@RequiredArgsConstructor
public class StrategyService {

    private final StrategyRegistry registry;

    /**
     * Вычисляем глобальный тип сигнала (BUY / SELL / HOLD).
     * 1) По StrategyType вытаскиваем нужный бин
     * 2) Смотрим сигналы конкретной стратегии (локальные – TradeStrategy.SignalType)
     * 3) Маппим локальный сигнал в глобальный SignalType
     */
    public SignalType calculateGlobalSignal(
            StrategyType type,
            List<Candle> candles,
            AiTradingSettings settings
    ) {
        // 1) Получили бин стратегии
        TradeStrategy strategy = registry.getByType(type);

        // 2) Вычисляем локальный сигнал
        TradeStrategy.SignalType localSignal = strategy.evaluate(candles, settings);

        // 3) Маппим локальный в глобальный
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
