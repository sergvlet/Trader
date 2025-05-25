// src/main/java/com/chicu/trader/strategy/rsiema/RsiEmaStrategy.java
package com.chicu.trader.strategy.rsiema;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.model.SignalType;
import com.chicu.trader.strategy.TradeStrategy;
import com.chicu.trader.trading.indicator.EmaCalculator;
import com.chicu.trader.trading.indicator.RsiCalculator;
import com.chicu.trader.trading.model.Candle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Стратегия RSI + EMA.
 * Условия:
 * — Покупка: RSI < threshold_buy и EMA_SHORT > EMA_LONG
 * — Продажа: RSI > threshold_sell и EMA_SHORT < EMA_LONG
 */
@Component
@RequiredArgsConstructor
public class RsiEmaStrategy implements TradeStrategy {

    private final RsiEmaStrategySettingsService configService;

    @Override
    public SignalType evaluate(List<Candle> candles, AiTradingSettings settings) {
        List<Double> closes = candles.stream()
                .map(Candle::getClose)
                .collect(Collectors.toList());

        RsiEmaStrategySettings cfg = configService.getOrCreate(settings.getChatId());

        double rsi = RsiCalculator.latest(closes, cfg.getRsiPeriod());
        double emaS = EmaCalculator.latest(closes, cfg.getEmaShort());
        double emaL = EmaCalculator.latest(closes, cfg.getEmaLong());

        if (rsi < cfg.getRsiBuyThreshold() && emaS > emaL) return SignalType.BUY;
        if (rsi > cfg.getRsiSellThreshold() && emaS < emaL) return SignalType.SELL;

        return SignalType.HOLD;
    }

    @Override
    public String code() {
        return "RSI_EMA";
    }

    @Override
    public String label() {
        return "RSI + EMA стратегия";
    }
}
