package com.chicu.trader.strategy.scalping;

import com.chicu.trader.strategy.SignalType;
import com.chicu.trader.strategy.StrategySettings;
import com.chicu.trader.strategy.StrategyType;
import com.chicu.trader.strategy.TradeStrategy;
import com.chicu.trader.strategy.scalping.model.ScalpingStrategySettings;
import com.chicu.trader.strategy.scalping.service.ScalpingStrategySettingsService;
import com.chicu.trader.trading.model.Candle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScalpingStrategy implements TradeStrategy {

    private final ScalpingStrategySettingsService settingsService;

    @Override
    public StrategyType getType() {
        return StrategyType.SCALPING;
    }

    @Override
    public SignalType evaluate(List<Candle> candles, StrategySettings strategySettings) {
        ScalpingStrategySettings cfg = (ScalpingStrategySettings) strategySettings;

        if (candles.size() < cfg.getWindowSize()) {
            return SignalType.HOLD;
        }

        List<Candle> recent = candles.subList(candles.size() - cfg.getWindowSize(), candles.size());
        double first = recent.get(0).getClose();
        double last = recent.get(recent.size() - 1).getClose();
        double change = ((last - first) / first) * 100;

        if (change >= cfg.getPriceChangeThreshold()) {
            return SignalType.BUY;
        } else if (change <= -cfg.getPriceChangeThreshold()) {
            return SignalType.SELL;
        }

        return SignalType.HOLD;
    }

    @Override
    public StrategySettings getSettings(Long chatId) {
        return settingsService.getOrCreate(chatId);
    }

    @Override
    public boolean isTrainable() {
        return false;
    }

    @Override
    public void train(Long chatId) {
        // Нет обучения для скальпинга
    }
}
