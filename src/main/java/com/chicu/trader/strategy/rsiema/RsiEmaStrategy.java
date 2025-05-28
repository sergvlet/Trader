// src/main/java/com/chicu/trader/strategy/rsiema/RsiEmaStrategy.java
package com.chicu.trader.strategy.rsiema;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.strategy.TradeStrategy;
import com.chicu.trader.strategy.StrategyType;
import com.chicu.trader.trading.indicator.EmaCalculator;
import com.chicu.trader.trading.indicator.RsiCalculator;
import com.chicu.trader.trading.model.Candle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RsiEmaStrategy implements TradeStrategy {

    private final RsiEmaStrategySettingsService configService;

    /**
     * Логика RSI+EMA:
     * BUY  — RSI < rsiBuyThreshold && emaShort > emaLong
     * SELL — RSI > rsiSellThreshold && emaShort < emaLong
     * иначе HOLD.
     */
    @Override
    public SignalType evaluate(List<Candle> candles, AiTradingSettings settings) {
        // Собираем цены закрытия
        List<Double> closes = candles.stream()
                .map(Candle::getClose)
                .collect(Collectors.toList());

        // Загружаем параметры стратегии для этого пользователя
        RsiEmaStrategySettings cfg = configService.getOrCreate(settings.getChatId());

        double rsi  = RsiCalculator.latest(closes, cfg.getRsiPeriod());
        double emaS = EmaCalculator.latest(closes, cfg.getEmaShort());
        double emaL = EmaCalculator.latest(closes, cfg.getEmaLong());

        if (rsi < cfg.getRsiBuyThreshold() && emaS > emaL) {
            return SignalType.BUY;
        }
        if (rsi > cfg.getRsiSellThreshold() && emaS < emaL) {
            return SignalType.SELL;
        }
        return SignalType.HOLD;
    }

    /** Тип этой стратегии — используется для регистрации в StrategyRegistry */
    @Override
    public StrategyType getType() {
        return StrategyType.RSI_EMA;
    }
}
