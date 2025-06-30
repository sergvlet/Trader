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
import java.util.Optional;

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

        double tpPct = Optional.ofNullable(settings.getRiskThreshold()).orElse(2.0);
        double slPct = Optional.ofNullable(settings.getMaxDrawdown()).orElse(1.0);
        double commission = Optional.ofNullable(settings.getCommission()).orElse(0.1);

        Duration interval = parseTimeframe(settings.getTimeframe());
        int limit = settings.getCachedCandlesLimit();

        for (String raw : settings.getSymbols().split(",")) {
            String symbol = raw.trim();
            if (symbol.isEmpty()) continue;

            List<Candle> candles = candleService.loadHistory(symbol, interval, limit);
            if (candles.size() < 10) continue;

            boolean open = false;
            Candle entry = null;

            for (int i = 10; i < candles.size(); i++) {
                List<Candle> history = candles.subList(0, i + 1);
                SignalType signal = strategy.evaluate(history, settings);
                Candle current = candles.get(i);

                if (signal == SignalType.BUY && !open) {
                    open = true;
                    entry = current;
                } else if (open && entry != null) {
                    double entryPrice = entry.getClose();
                    double high = current.getHigh();
                    double low = current.getLow();

                    double tp = entryPrice * (1 + tpPct / 100.0);
                    double sl = entryPrice * (1 - slPct / 100.0);

                    if (high >= tp || low <= sl) {
                        double exitPrice = high >= tp ? tp : sl;
                        result.addTrade(new BacktestResult.Trade(
                                symbol,
                                entry.getCloseTime(),
                                entryPrice,
                                current.getCloseTime(),
                                exitPrice,
                                commission
                        ));
                        open = false;
                        entry = null;
                    }
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
