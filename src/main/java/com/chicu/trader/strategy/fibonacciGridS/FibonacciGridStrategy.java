package com.chicu.trader.strategy.fibonacciGridS;

import com.chicu.trader.strategy.SignalType;
import com.chicu.trader.strategy.StrategySettings;
import com.chicu.trader.strategy.StrategyType;
import com.chicu.trader.strategy.TradeStrategy;
import com.chicu.trader.strategy.fibonacciGridS.model.FibonacciGridStrategySettings;
import com.chicu.trader.strategy.fibonacciGridS.service.FibonacciGridStrategySettingsService;
import com.chicu.trader.trading.model.Candle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class FibonacciGridStrategy implements TradeStrategy {

    private final FibonacciGridStrategySettingsService settingsService;

    @Override
    public StrategyType getType() {
        return StrategyType.FIBONACCI_GRID;
    }

    @Override
    public SignalType evaluate(List<Candle> candles, StrategySettings rawSettings) {
        if (candles == null || candles.isEmpty()) return SignalType.HOLD;
        if (!(rawSettings instanceof FibonacciGridStrategySettings settings)) return SignalType.HOLD;

        BigDecimal currentPrice = BigDecimal.valueOf(candles.get(candles.size() - 1).getClose());
        BigDecimal basePrice = BigDecimal.valueOf(candles.get(0).getClose()); // базовая цена — первая свеча

        if (isBuySignal(currentPrice, basePrice, settings)) return SignalType.BUY;
        if (isSellSignal(currentPrice, basePrice, settings)) return SignalType.SELL;

        return SignalType.HOLD;
    }

    private boolean isBuySignal(BigDecimal currentPrice, BigDecimal basePrice, FibonacciGridStrategySettings settings) {
        return IntStream.rangeClosed(1, settings.getGridLevels())
                .mapToObj(i -> priceAtLevel(basePrice, settings.getDistancePct(), i, true))
                .anyMatch(level -> currentPrice.compareTo(level) >= 0);
    }

    private boolean isSellSignal(BigDecimal currentPrice, BigDecimal basePrice, FibonacciGridStrategySettings settings) {
        return IntStream.rangeClosed(1, settings.getGridLevels())
                .mapToObj(i -> priceAtLevel(basePrice, settings.getDistancePct(), i, false))
                .anyMatch(level -> currentPrice.compareTo(level) <= 0);
    }

    private BigDecimal priceAtLevel(BigDecimal basePrice, double distancePct, int level, boolean isUpward) {
        BigDecimal percent = BigDecimal.valueOf(distancePct * level / 100.0);
        BigDecimal multiplier = isUpward
                ? BigDecimal.ONE.add(percent)
                : BigDecimal.ONE.subtract(percent);
        return basePrice.multiply(multiplier).setScale(8, RoundingMode.HALF_UP);
    }

    @Override
    public FibonacciGridStrategySettings getSettings(Long chatId) {
        return settingsService.getOrCreate(chatId);
    }
}
