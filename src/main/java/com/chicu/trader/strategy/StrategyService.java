package com.chicu.trader.strategy;

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
            Long chatId,
            List<Candle> candles
    ) {
        TradeStrategy strategy = registry.getStrategy(type);
        StrategySettings settings = strategy.getSettings(chatId);

        SignalType localSignal = strategy.evaluate(candles, settings);

        return switch (localSignal) {
            case BUY -> SignalType.BUY;
            case SELL -> SignalType.SELL;
            default -> SignalType.HOLD;
        };
    }
}
