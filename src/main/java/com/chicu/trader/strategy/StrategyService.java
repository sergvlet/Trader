package com.chicu.trader.strategy;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.model.SignalType;
import com.chicu.trader.trading.model.Candle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис для выбора и исполнения нужной стратегии.
 */
@Service
@RequiredArgsConstructor
public class StrategyService {

    private final StrategyRegistry registry;

    public SignalType evaluate(Long chatId, List<Candle> candles, AiTradingSettings settings) {
        String code = settings.getStrategy();
        TradeStrategy strategy = registry.findByCode(code);
        return strategy.evaluate(candles, settings);
    }
}
