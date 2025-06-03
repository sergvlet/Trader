package com.chicu.trader.strategy;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.trading.model.Candle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StrategyService {

    private final StrategyRegistry registry;

    public SignalType calculateGlobalSignal(
            StrategyType type,
            List<Candle> candles,
            AiTradingSettings settings
    ) {
        TradeStrategy strategy = registry.getByType(type);

        SignalType localSignal = strategy.evaluate(candles, settings);

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
