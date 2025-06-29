package com.chicu.trader.trading.service.impl;

import com.chicu.trader.bot.entity.AiTradingSettings;
import com.chicu.trader.bot.service.AiTradingSettingsService;
import com.chicu.trader.trading.model.BacktestResult;

import com.chicu.trader.trading.model.Candle;
import com.chicu.trader.trading.service.BacktestService;
import com.chicu.trader.trading.service.CandleService;
import com.chicu.trader.strategy.SignalType;
import com.chicu.trader.strategy.TradeStrategy;
import com.chicu.trader.strategy.StrategyRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BacktestServiceImpl implements BacktestService {

    private final AiTradingSettingsService settingsService;
    private final CandleService candleService;
    private final StrategyRegistry strategyRegistry;

    @Override
    public BacktestResult runBacktest(Long chatId) {
        AiTradingSettings settings = settingsService.getSettingsOrThrow(chatId);
        BacktestResult result = new BacktestResult();
        TradeStrategy strategy = strategyRegistry.getStrategy(settings.getStrategy());

        for (String symRaw : settings.getSymbols().split(",")) {
            String symbol = symRaw.trim();
            if (symbol.isEmpty()) continue;

            Duration interval = parseTimeframe(settings.getTimeframe());
            int limit = settings.getCachedCandlesLimit();
            List<Candle> candles = candleService.loadHistory(symbol, interval, limit);

            boolean positionOpen = false;
            Candle entryCandle = null;

            for (int i = 0; i < candles.size(); i++) {
                List<Candle> past = candles.subList(0, i + 1);
                SignalType signal = strategy.evaluate(past, settings);

                if (signal == SignalType.BUY && !positionOpen) {
                    positionOpen = true;
                    entryCandle = candles.get(i);
                } else if (signal == SignalType.SELL && positionOpen && entryCandle != null) {
                    Candle exitCandle = candles.get(i);
                    result.addTrade(new BacktestResult.Trade(
                        entryCandle.getCloseTime(),
                        entryCandle.getClose(),
                        exitCandle.getCloseTime(),
                        exitCandle.getClose()
                    ));
                    positionOpen = false;
                }
            }
        }

        return result;
    }

    private Duration parseTimeframe(String tf) {
        tf = tf.trim().toLowerCase();
        long value = Long.parseLong(tf.substring(0, tf.length() - 1));
        char unit = tf.charAt(tf.length() - 1);
        return switch (unit) {
            case 'm' -> Duration.ofMinutes(value);
            case 'h' -> Duration.ofHours(value);
            case 'd' -> Duration.ofDays(value);
            default -> throw new IllegalArgumentException("Unsupported timeframe: " + tf);
        };
    }
}
